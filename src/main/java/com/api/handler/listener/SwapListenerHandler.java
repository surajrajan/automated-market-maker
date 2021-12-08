package com.api.handler.listener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.client.kms.token.SwapClaimToken;
import com.model.SwapEstimate;
import com.model.SwapRequest;
import com.model.Transaction;
import com.model.exception.InvalidInputException;
import com.model.types.TransactionStatus;
import com.util.LiquidityPoolUtil;
import com.util.ObjectMapperUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Setter
public class SwapListenerHandler implements RequestHandler<SQSEvent, Void> {

    private DynamoDBClient dynamoDBClient;
    private MarketMakerLogic marketMakerLogic;

    public SwapListenerHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
        this.marketMakerLogic = new MarketMakerLogic();
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        log.info("Received request: {}", sqsEvent);

        // parse swap claim details and load liquidity pool details
        final SwapClaimToken swapClaimToken;
        final LiquidityPool liquidityPool;
        final SwapRequest swapRequest;
        try {
            // only one message at a time
            final String body = sqsEvent.getRecords().get(0).getBody();
            log.info("SQS message body: {}", body);
            swapClaimToken = ObjectMapperUtil.toClass(body, SwapClaimToken.class);
            swapRequest = swapClaimToken.getSwapRequest();
            final String liquidityPoolName = LiquidityPoolUtil
                    .getLiquidityPoolName(swapRequest.getAssetNameIn(), swapRequest.getAssetNameOut());
            liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);
        } catch (InvalidInputException e) {
            log.error("Message is invalid format. Failed. Deleting.", e);
            return null;
        }

        final String swapContractId = swapClaimToken.getSwapContractId();
        log.info("Processing swapContractId: {}", swapContractId);

        // calculate a new swap estimate, which may be different from what initially estimated
        // note, the swap estimate is constructed again here to get the final actual estimate before writing
        // eventually, a "slippage" parameter could be provided to execute or not execute
        final SwapEstimate swapEstimate = marketMakerLogic.createSwapEstimate(liquidityPool, swapRequest);
        final LiquidityPool newLiquidityPool = marketMakerLogic.applySwapEstimateToPool(swapEstimate, liquidityPool);

        // construct the transaction, containing before and after details of the liquidity pool
        final Transaction transaction = new Transaction();
        Date now = new Date();

        // construct transactionId as the swapContractId
        transaction.setTransactionId(swapContractId);
        transaction.setTransactionState(TransactionStatus.FINISHED.name());
        transaction.setSwapEstimate(swapEstimate);
        transaction.setTimeCompleted(now);

        // save the transaction and liquidity pool update
        dynamoDBClient.writeTransactionAndUpdateLiquidityPool(transaction, newLiquidityPool);
        log.info("Updated transaction and pool in dynamoDB.");
        return null;
    }
}
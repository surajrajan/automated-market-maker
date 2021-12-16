package com.api.handler.listener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.token.SwapClaimToken;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.model.SwapEstimate;
import com.model.SwapRequest;
import com.model.Transaction;
import com.model.types.TransactionStatus;
import com.util.LiquidityPoolUtil;
import com.util.ObjectMapperUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * Handler to listen to the SQS queue for swaps. The messages in this queue are of object SwapClaimToken, which contains
 * the original SwapRequest and transactionId. The logic is as follows:
 * 1. Deserialize the SQS message into the SwapClaimToken object and load the LiquidityPool details. If there are
 *    any issues with this step, the message is discarded, as that implies that the message in the queue was not put
 *    using the SubmitSwap API.
 * 2. Generate a **new** SwapEstimate and LiquidityPool after-state for the current time.
 * 3. Update both the associated Transaction to complete and LiquidityPool details in DynamoDB.
 */
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
            final String poolName = LiquidityPoolUtil
                    .getPoolName(swapRequest.getInName(), swapRequest.getOutName());
            liquidityPool = dynamoDBClient.loadLiquidityPool(poolName);
        } catch (Exception e) {
            log.error("Message is invalid format. Ignoring message and processing / deleting.", e);
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
        transaction.setSwapApplied(swapEstimate);
        transaction.setTimeCompleted(now);

        // save the transaction and liquidity pool update
        dynamoDBClient.writeTransactionAndUpdateLiquidityPool(transaction, newLiquidityPool);
        log.info("Updated transaction and pool in dynamoDB.");
        return null;
    }
}

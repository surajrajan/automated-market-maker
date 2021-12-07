package com.api.handler.listener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.model.SwapContract;
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
public class SwapRequestListenerHandler implements RequestHandler<SQSEvent, Void> {

    private DynamoDBClient dynamoDBClient;

    public SwapRequestListenerHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        log.info("Received request: {}", sqsEvent);

        final SwapContract swapContract;
        final LiquidityPool liquidityPool;
        try {
            // only one message at a time
            final String body = sqsEvent.getRecords().get(0).getBody();
            log.info("SQS message body: {}", body);
            swapContract = ObjectMapperUtil.toClass(body, SwapContract.class);
            final String liquidityPoolName = LiquidityPoolUtil
                    .getLiquidityPoolName(swapContract.getInName(), swapContract.getOutName());
            liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);
        } catch (InvalidInputException e) {
            log.error("Message is invalid format. Failed. Deleting.", e);
            return null;
        }

        final String swapContractId = swapContract.getSwapContractId();
        log.info("Processing swapContractId: {}", swapContractId);

        // determine updated pool after swap occurs
        final LiquidityPool newLiquidityPool = MarketMakerLogic.applySwapToPool(swapContract, liquidityPool);
        log.info("Updated liquidity pool: {}", newLiquidityPool);

        // construct the transaction, containing before and after details of the liquidity pool
        final Transaction transaction = new Transaction();
        Date now = new Date();
        // construct transactionId as the swapContractId
        transaction.setTransactionId(swapContractId);
        transaction.setTransactionState(TransactionStatus.FINISHED.name());
        transaction.setSwapContract(swapContract);
        transaction.setBeforeState(liquidityPool);
        transaction.setAfterState(newLiquidityPool);
        transaction.setTimeCompleted(now);

        // save the transaction and liquidity pool update
        dynamoDBClient.writeTransaction(transaction, newLiquidityPool);
        log.info("Updated transaction and pool in dynamoDB.");
        return null;
    }
}

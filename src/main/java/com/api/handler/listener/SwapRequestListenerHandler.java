package com.api.handler.listener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.model.SwapContract;
import com.model.SwapRequest;
import com.model.Transaction;
import com.util.LiquidityPoolUtil;
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

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        log.info("Received request: {}", sqsEvent);
        SwapRequest swapRequest = null;
        try {
            // only one message at a time
            String body = sqsEvent.getRecords().get(0).getBody();
            swapRequest = objectMapper.readValue(body, SwapRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Message is invalid format. Failed. Deleting.", e);
            return null;
        }

        Date now = new Date();
        SwapContract swapContract = swapRequest.getSwapContract();
        if (now.after(swapContract.getExpiresAt())) {
            log.error("Claim was expired. Failed. Deleting");
            return null;
        }

        // load existing pool
        final String transactionId = swapRequest.getTransactionId();
        log.info("Processing transactionId: {}", transactionId);
        String liquidityPoolName = LiquidityPoolUtil
                .getLiquidityPoolName(swapContract.getInName(), swapContract.getOutName());
        final LiquidityPool liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);

        // determine updated pool if swap is handled
        final LiquidityPool newLiquidityPool = MarketMakerLogic.applySwapToPool(swapContract, liquidityPool);
        log.info("Updated liquidity pool: {}", newLiquidityPool);

        // construct the transaction, containing before and after details of the liquidity pool
        final Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionState("Completed");
        transaction.setBeforeState(liquidityPool);
        transaction.setAfterState(newLiquidityPool);
        transaction.setSwapContract(swapContract);
        transaction.setTimeCompleted(now);

        // save the transaction and liquidity pool update
        dynamoDBClient.writeTransaction(transaction, newLiquidityPool);
        log.info("Updated transaction and pool in dynamoDB.");
        return null;
    }
}

package com.client.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.config.ErrorMessages;
import com.model.LiquidityPool;
import com.model.SwapContract;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Slf4j
public class DynamoDBClient {

    private DynamoDBMapper dynamoDBMapper;

    public void saveLiquidityPool(final LiquidityPool liquidityPool) {
        try {
            DynamoDBSaveExpression saveExpr = new DynamoDBSaveExpression();
            Map<String, ExpectedAttributeValue> expectedAttributeValueMap = new HashMap<>();
            // fail if item already exists
            expectedAttributeValueMap.put(DBConstants.LIQUIDITY_POOL_NAME_KEY, new ExpectedAttributeValue(false));
            saveExpr.setExpected(expectedAttributeValueMap);
            dynamoDBMapper.save(liquidityPool, saveExpr);
        } catch (ConditionalCheckFailedException e) {
            throw new IllegalArgumentException(ErrorMessages.LIQUIDITY_POOL_ALREADY_EXISTS);
        }
    }

    public LiquidityPool loadLiquidityPool(final String liquidityPoolName) {
        log.info("Getting liquidity pool with name: {}", liquidityPoolName);
        try {
            LiquidityPool liquidityPool = dynamoDBMapper.load(LiquidityPool.class, liquidityPoolName);
            log.info("Returned: {}", liquidityPool);
            if (liquidityPool == null) {
                throw new IllegalArgumentException(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME);
            }
            return liquidityPool;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public String putPendingTransaction(final SwapContract swapContract) {
        String transactionId = UUID.randomUUID().toString();

        return transactionId;
    }
}

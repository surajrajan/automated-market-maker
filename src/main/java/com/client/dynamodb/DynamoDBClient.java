package com.client.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.config.ErrorMessages;
import com.model.LiquidityPool;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Slf4j
public class DynamoDBClient {

    private DynamoDBMapper dynamoDBMapper;

    public void saveLiquidityPool(final LiquidityPool liquidityPool) {
        try {
            DynamoDBSaveExpression saveExpr = new DynamoDBSaveExpression();
            Map<String, ExpectedAttributeValue> expectedAttributeValueMap = new HashMap<>();
            expectedAttributeValueMap.put(DBConstants.LIQUIDITY_POOL_NAME_KEY, new ExpectedAttributeValue(false));
            saveExpr.setExpected(expectedAttributeValueMap);
            dynamoDBMapper.save(liquidityPool, saveExpr);
        } catch (ConditionalCheckFailedException e) {
            throw new IllegalArgumentException(ErrorMessages.LIQUIDITY_POOL_ALREADY_EXISTS);
        }
    }

    public LiquidityPool loadLiquidityPool(final String liquidityPoolName) {
        log.info("loading for {} ", liquidityPoolName);
        try {
            return dynamoDBMapper.load(LiquidityPool.class, liquidityPoolName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getCause());
        }
    }
}

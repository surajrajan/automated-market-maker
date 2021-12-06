package com.client.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.config.ErrorMessages;
import com.model.LiquidityPool;
import com.model.Transaction;
import com.model.exception.InvalidInputException;
import com.model.types.TransactionStatus;
import com.util.LiquidityPoolUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Slf4j
@Singleton
public class DynamoDBClient {

    private DynamoDBMapper dynamoDBMapper;

    /**
     * Configures the ability to skip null attributes when making an update. Ex - when an item is updated
     * a second time, and only some fields need to be updated (ignoring existing fields).
     */
    private static DynamoDBMapperConfig SKIP_NULL_ATTRS_WRITE_CONFIG = DynamoDBMapperConfig.builder()
            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES).build();

    /**
     * Creates a liquidity pool.
     *
     * @param liquidityPool
     * @throws InvalidInputException when poolName (hashKey) already exists
     */
    public void createLiquidityPool(final LiquidityPool liquidityPool) throws InvalidInputException {
        try {
            log.info("Creating liquidity pool: {}", liquidityPool);
            DynamoDBSaveExpression saveExpr = new DynamoDBSaveExpression();
            Map<String, ExpectedAttributeValue> expectedAttributeValueMap = new HashMap<>();
            // fail if item already exists
            expectedAttributeValueMap.put(DBConstants.LIQUIDITY_POOL_NAME_KEY, new ExpectedAttributeValue(false));
            saveExpr.setExpected(expectedAttributeValueMap);
            dynamoDBMapper.save(liquidityPool, saveExpr);
        } catch (ConditionalCheckFailedException e) {
            log.error(e.getMessage(), e);
            throw new InvalidInputException(e);
        }
    }

    /**
     * Loads an existing liquidity pool by hash key liquidityPoolName.
     *
     * @param liquidityPoolName
     * @return LiquidityPool
     * @throws InvalidInputException if poolName (hashKey) does not exist
     */
    public LiquidityPool loadLiquidityPool(final String liquidityPoolName) throws InvalidInputException {
        log.info("Getting liquidity pool with name: {}", liquidityPoolName);
        LiquidityPool liquidityPool = dynamoDBMapper.load(LiquidityPool.class, liquidityPoolName);
        log.info("Returned: {}", liquidityPool);
        LiquidityPoolUtil.logKValue(liquidityPool);
        if (liquidityPool == null) {
            throw new InvalidInputException(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME + "- " + liquidityPoolName);
        }
        return liquidityPool;
    }

    public String initializeTransaction() {
        final Transaction transaction = new Transaction();
        final String transactionId = UUID.randomUUID().toString();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionState(TransactionStatus.STARTED.name());
        Date now = new Date();
        transaction.setTimeStarted(now);
        dynamoDBMapper.save(transaction);
        return transactionId;
    }

    public void writeTransaction(final Transaction transaction,
                                 final LiquidityPool newLiquidityPool) {
        dynamoDBMapper.save(transaction, SKIP_NULL_ATTRS_WRITE_CONFIG);
        dynamoDBMapper.save(newLiquidityPool);
    }
}

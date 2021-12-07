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
     * Configures the ability to fail when saving if an existing liquidityPool name already exists.
     */
    private static DynamoDBSaveExpression FAIL_ON_EXISTING_LIQUIDITY_POOL = new DynamoDBSaveExpression()
            .withExpected(new HashMap<String, ExpectedAttributeValue>() {{
                              put(DBConstants.LIQUIDITY_POOL_NAME_KEY, new ExpectedAttributeValue(false));
                          }}
            );

    /**
     * Configures the ability to fail when saving if an existing transactionId already exists.
     */
    private static DynamoDBSaveExpression FAIL_ON_EXISTING_TRANSACTION = new DynamoDBSaveExpression()
            .withExpected(new HashMap<String, ExpectedAttributeValue>() {{
                              put(DBConstants.TRANSACTION_ID_KEY, new ExpectedAttributeValue(false));
                          }}
            );

    /**
     * Creates a liquidity pool.
     *
     * @param liquidityPool
     * @throws InvalidInputException when poolName (hashKey) already exists
     */
    public void createLiquidityPool(final LiquidityPool liquidityPool) throws InvalidInputException {
        try {
            log.info("Creating liquidity pool: {}", liquidityPool);
            dynamoDBMapper.save(liquidityPool, FAIL_ON_EXISTING_LIQUIDITY_POOL);
        } catch (ConditionalCheckFailedException e) {
            log.error("Liquidity pool already exists.");
            throw new InvalidInputException(ErrorMessages.LIQUIDITY_POOL_ALREADY_EXISTS, e);
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

    public void initializeTransaction(final String transactionId) throws InvalidInputException {
        try {
            log.info("Initializing transaction: {}", transactionId);
            final Transaction transaction = new Transaction();
            transaction.setTransactionId(transactionId);
            transaction.setTransactionState(TransactionStatus.STARTED.name());
            Date now = new Date();
            transaction.setTimeStarted(now);
            dynamoDBMapper.save(transaction, FAIL_ON_EXISTING_TRANSACTION);
        } catch (ConditionalCheckFailedException e) {
            log.error("Claim already used.");
            throw new InvalidInputException(e);
        }
    }

    public void writeTransaction(final Transaction transaction,
                                 final LiquidityPool newLiquidityPool) {
        dynamoDBMapper.save(transaction, SKIP_NULL_ATTRS_WRITE_CONFIG);
        dynamoDBMapper.save(newLiquidityPool);
    }
}

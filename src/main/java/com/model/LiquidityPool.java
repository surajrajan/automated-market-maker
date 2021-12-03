package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.client.dynamodb.DBConstants;
import lombok.Builder;
import lombok.Data;

/**
 * Set of all unique pairs.
 */
@Builder
@Data
@DynamoDBTable(tableName = DBConstants.LIQUIDITY_POOLS_TABLE_NAME)
public class LiquidityPool {

    @DynamoDBHashKey(attributeName = DBConstants.LIQUIDITY_POOL_NAME_KEY)
    private String liquidityPoolName;

    private Double assetOneLocalPrice;
    private Double assetOneSupply;

    private Double assetTwoLocalPrice;
    private Double assetTwoSupply;
}

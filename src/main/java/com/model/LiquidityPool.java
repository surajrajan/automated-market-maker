package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.client.dynamodb.DBConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Set of all unique pairs.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDBTable(tableName = DBConstants.LIQUIDITY_POOLS_TABLE_NAME)
@DynamoDBDocument
public class LiquidityPool {

    @DynamoDBHashKey(attributeName = DBConstants.LIQUIDITY_POOL_NAME_KEY)
    private String liquidityPoolName;

    private AssetAmount assetOne;
    private AssetAmount assetTwo;
}

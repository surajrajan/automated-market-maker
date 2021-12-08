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
 * Data about a liquidity pool between two assets. Contains the name in alphabetical order (ex - Apples-Bananas) and the
 * AssetAmount of each asset.
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

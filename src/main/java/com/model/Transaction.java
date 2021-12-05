package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.client.dynamodb.DBConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDBTable(tableName = DBConstants.TRANSACTIONS_TABLE_NAME)
public class Transaction {
    @DynamoDBHashKey(attributeName = DBConstants.TRANSACTION_ID_KEY)
    private String transactionId;
    private String transactionState;
    private LiquidityPool before;
    private LiquidityPool after;
    private SwapContract swapContract;
    private Date timeStarted;
    private Date timeCompleted;
}

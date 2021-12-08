package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.client.dynamodb.DBConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Entity to capture a transaction / swap being made. Initialized based on a unique SwapEstimate swapContractId (requestId)
 * and contains the swap estimate details that were performed. Also contains the status, which is maintained when
 * a swap is submitted and then finally completed / processed.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@DynamoDBTable(tableName = DBConstants.TRANSACTIONS_TABLE_NAME)
public class Transaction {
    @DynamoDBHashKey(attributeName = DBConstants.TRANSACTION_ID_KEY)
    private String transactionId;
    private String transactionState;
    private SwapEstimate swapEstimate;
    private Date timeStarted;
    private Date timeCompleted;
}

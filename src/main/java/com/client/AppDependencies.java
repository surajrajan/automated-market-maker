package com.client;

import com.client.dynamodb.DynamoDBClient;
import com.client.dynamodb.DynamoDBModule;
import com.client.kms.KMSClient;
import com.client.kms.KMSModule;
import com.client.sqs.SQSClient;
import com.client.sqs.SQSModule;
import dagger.Component;

@Component(modules = {DynamoDBModule.class, SQSModule.class, KMSModule.class})
public interface AppDependencies {
    DynamoDBClient dynamoDBClient();

    KMSClient kmsClient();

    SQSClient sqsClient();
}

package com.client.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import dagger.Module;
import dagger.Provides;

@Module
public class DynamoDBModule {

    @Provides
    DynamoDBMapper dynamoDBMapper() {
        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClient.builder().build();
        return new DynamoDBMapper(dynamoDBClient);
    }

    @Provides
    DynamoDBClient dynamoDBClient() {
        return new DynamoDBClient(dynamoDBMapper());
    }
}

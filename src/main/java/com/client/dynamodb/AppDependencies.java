package com.client.dynamodb;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = DynamoDBModule.class)
public interface AppDependencies {
    DynamoDBClient dynamoDBClient();
}

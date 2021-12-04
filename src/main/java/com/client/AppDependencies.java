package com.client;

import com.client.dynamodb.DynamoDBClient;
import com.client.dynamodb.DynamoDBModule;
import com.client.jwt.JWTClient;
import com.client.jwt.JWTModule;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {DynamoDBModule.class, JWTModule.class})
public interface AppDependencies {
    DynamoDBClient dynamoDBClient();

    JWTClient jwtClient();
}

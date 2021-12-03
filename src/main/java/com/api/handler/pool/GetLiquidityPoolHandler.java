package com.api.handler.pool;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.client.dynamodb.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.config.ErrorMessages;
import com.model.LiquidityPool;
import com.serverless.ApiGatewayResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Setter
public class GetLiquidityPoolHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;

    public GetLiquidityPoolHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        String liquidityPoolName = extractLiquidityPoolNameFromPathParam(input);

        try {
            LiquidityPool liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);
            if (liquidityPool == null) {
                return ApiGatewayResponse.createBadRequest(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME, context);
            }
            return ApiGatewayResponse.createSuccessResponse(liquidityPool, context);

        } catch (IllegalArgumentException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

    }

    private String extractLiquidityPoolNameFromPathParam(final Map<String, Object> input) {
        try {
            Map<String, String> pathParameters = (Map<String, String>) input.get("pathParameters");
            String liquidityPoolName = pathParameters.get("liquidityPoolName");
            return liquidityPoolName;
        } catch (Exception e) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
    }

}

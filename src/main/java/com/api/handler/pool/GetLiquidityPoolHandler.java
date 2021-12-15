package com.api.handler.pool;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.config.ServiceConstants;
import com.model.LiquidityPool;
import com.model.exception.InvalidInputException;
import com.serverless.ApiGatewayResponse;
import com.util.LiquidityPoolUtil;
import com.util.RequestUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for CreateLiquidityPool API.
 * Requires liquidity pool name from the path.
 * Gets the liquidity pool in DynamoDB.
 */
@Slf4j
@Setter
public class GetLiquidityPoolHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;

    public GetLiquidityPoolHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        log.info("Received request event: {}", requestEvent);
        try {
            final String poolName = RequestUtil.extractPoolNameFromPathParams(requestEvent.getPathParameters(),
                    ServiceConstants.LIQUIDITY_POOL_PATH_PARAMETER_NAME);
            LiquidityPoolUtil.validateLiquidityPoolName(poolName);
            final LiquidityPool liquidityPool = dynamoDBClient.loadLiquidityPool(poolName);
            return ApiGatewayResponse.createSuccessResponse(liquidityPool, context);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }
    }
}

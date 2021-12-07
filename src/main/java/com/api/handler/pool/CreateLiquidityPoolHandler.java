package com.api.handler.pool;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.api.handler.pool.model.CreateLiquidityPoolRequest;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.config.ErrorMessages;
import com.config.ServiceLimits;
import com.model.AssetAmount;
import com.model.LiquidityPool;
import com.model.exception.InvalidInputException;
import com.serverless.ApiGatewayResponse;
import com.util.LiquidityPoolUtil;
import com.util.ObjectMapperUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * Handler for CreateLiquidityPool API.
 * <p>
 * Creates a liquidity pool.
 */
@Slf4j
@Setter
public class CreateLiquidityPoolHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;

    public CreateLiquidityPoolHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        log.info("Received request event: {}", requestEvent);
        final String body = requestEvent.getBody();
        final CreateLiquidityPoolRequest request;
        final String liquidityPoolName;
        // validations
        List<AssetAmount> assetAmountList;
        try {
            liquidityPoolName = LiquidityPoolUtil.extractLiquidityPoolNameFromPathParams(requestEvent.getPathParameters());
            request = ObjectMapperUtil.toClass(body, CreateLiquidityPoolRequest.class);
            assetAmountList = Arrays.asList(request.getAssetAmountOne(), request.getAssetAmountTwo());
            validateBounds(assetAmountList);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // create internal LiquidityPool object to save in DB
        log.info("Inputs valid. Creating pool with name: {}", liquidityPoolName);
        LiquidityPool liquidityPool = LiquidityPool.builder()
                .liquidityPoolName(liquidityPoolName)
                .assetOne(AssetAmount.builder()
                        .amount(request.getAssetAmountOne().getAmount())
                        .price(request.getAssetAmountOne().getPrice())
                        .build())
                .assetTwo(AssetAmount.builder()
                        .amount(request.getAssetAmountTwo().getAmount())
                        .price(request.getAssetAmountTwo().getPrice())
                        .build())
                .build();

        // save into dynamoDB
        try {
            dynamoDBClient.createLiquidityPool(liquidityPool);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // return success / created / 201
        return ApiGatewayResponse.createEmptyResponse(201, context);
    }


    private void validateBounds(final List<AssetAmount> assetAmountList) throws InvalidInputException {
        // ensure valid numbers
        for (final AssetAmount assetAmount : assetAmountList) {
            if (assetAmount.getPrice() < ServiceLimits.MIN_PRICE
                    || assetAmount.getPrice() > ServiceLimits.MAX_PRICE) {
                throw new InvalidInputException(ErrorMessages.INVALID_PRICE_RANGE);
            }
            if (assetAmount.getAmount() < ServiceLimits.MIN_SUPPLY
                    || assetAmount.getAmount() > ServiceLimits.MAX_SUPPLY) {
                throw new InvalidInputException(ErrorMessages.INVALID_SUPPLY_RANGE);
            }
        }

        // validate equal market cap
        AssetAmount assetAmountOne = assetAmountList.get(0);
        AssetAmount assetAmountTwo = assetAmountList.get(1);
        if (assetAmountOne.getPrice() * assetAmountOne.getAmount() !=
                assetAmountTwo.getPrice() * assetAmountTwo.getAmount()) {
            throw new InvalidInputException(ErrorMessages.UNEQUAL_MARKET_CAP_LIQUIDITY_UPDATE);
        }
    }
}

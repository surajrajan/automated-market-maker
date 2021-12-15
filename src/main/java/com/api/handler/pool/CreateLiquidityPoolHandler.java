package com.api.handler.pool;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.api.handler.pool.model.CreateLiquidityPoolRequest;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.config.ErrorMessages;
import com.config.ServiceConstants;
import com.model.LiquidityPool;
import com.model.PriceAmount;
import com.model.exception.InvalidInputException;
import com.serverless.ApiGatewayResponse;
import com.util.LiquidityPoolUtil;
import com.util.ObjectMapperUtil;
import com.util.RequestUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Handler for CreateLiquidityPool API.
 * Requires liquidity pool name from the path and a CreateLiquidityPoolRequest body containing initial asset amount details.
 * Creates / saves the liquidity pool in DynamoDB.
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
        final String poolName;
        // validations
        List<PriceAmount> priceAmountList;
        try {
            poolName = RequestUtil.extractPoolNameFromPathParams(requestEvent.getPathParameters(),
                    ServiceConstants.LIQUIDITY_POOL_PATH_PARAMETER_NAME);
            LiquidityPoolUtil.validateLiquidityPoolName(poolName);
            final CreateLiquidityPoolRequest request = ObjectMapperUtil.toClass(requestEvent.getBody(), CreateLiquidityPoolRequest.class);
            priceAmountList = Arrays.asList(request.getAssetOne(), request.getAssetTwo());
            validatePriceAmounts(priceAmountList);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // create LiquidityPool object to save in DB
        Date currentTime = new Date();
        log.info("Inputs valid. Creating pool with name: {}", poolName);
        PriceAmount priceAmountOne = priceAmountList.get(0);
        PriceAmount priceAmountTwo = priceAmountList.get(1);
        LiquidityPool liquidityPool = LiquidityPool.builder()
                .poolName(poolName)
                .assetOne(PriceAmount.builder()
                        .amount(priceAmountOne.getAmount())
                        .price(priceAmountOne.getPrice())
                        .build())
                .assetTwo(PriceAmount.builder()
                        .amount(priceAmountTwo.getAmount())
                        .price(priceAmountTwo.getPrice())
                        .build())
                .createdTime(currentTime)
                .updatedTime(currentTime)
                .build();

        // save into dynamoDB
        try {
            dynamoDBClient.createLiquidityPool(liquidityPool);
        } catch (InvalidInputException e) {
            // liquidity pool already exists
            return ApiGatewayResponse.createBadRequest(ErrorMessages.LIQUIDITY_POOL_ALREADY_EXISTS, context);
        }

        // return success / created / 201
        return ApiGatewayResponse.createEmptyResponse(201, context);
    }


    /**
     * Validates that the PriceAmount objects have valid price and supply ranges. Also, the market cap (supply x price)
     * of each asset must be equal. If not, throws InvalidInputException.
     *
     * @param priceAmountList
     * @throws InvalidInputException
     */
    private void validatePriceAmounts(final List<PriceAmount> priceAmountList) throws InvalidInputException {
        // ensure valid numbers
        for (final PriceAmount priceAmount : priceAmountList) {
            if (priceAmount.getPrice() < ServiceConstants.MIN_PRICE
                    || priceAmount.getPrice() > ServiceConstants.MAX_PRICE) {
                throw new InvalidInputException(ErrorMessages.INVALID_PRICE_RANGE);
            }
            if (priceAmount.getAmount() < ServiceConstants.MIN_SUPPLY
                    || priceAmount.getAmount() > ServiceConstants.MAX_SUPPLY) {
                throw new InvalidInputException(ErrorMessages.INVALID_SUPPLY_RANGE);
            }
        }

        // validate equal market cap
        PriceAmount priceAmountOne = priceAmountList.get(0);
        PriceAmount priceAmountTwo = priceAmountList.get(1);
        if (priceAmountOne.getPrice() * priceAmountOne.getAmount() !=
                priceAmountTwo.getPrice() * priceAmountTwo.getAmount()) {
            throw new InvalidInputException(ErrorMessages.UNEQUAL_MARKET_CAP_LIQUIDITY_UPDATE);
        }
    }
}

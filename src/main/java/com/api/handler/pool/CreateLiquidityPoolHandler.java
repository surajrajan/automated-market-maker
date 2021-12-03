package com.api.handler.pool;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.client.dynamodb.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.config.ErrorMessages;
import com.config.ServiceConstants;
import com.config.ServiceLimits;
import com.model.LiquidityPool;
import com.serverless.ApiGatewayResponse;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Handler for CreateLiquidityPool API.
 * <p>
 * Creates a liquidity pool for the invoking user.
 */
@Slf4j
@Setter
public class CreateLiquidityPoolHandler implements RequestHandler<CreateLiquidityPoolHandler.CreateLiquidityPoolRequest, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;

    public CreateLiquidityPoolHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(CreateLiquidityPoolRequest input, Context context) {
        log.info("Received request: {}", input);
        // validate and collect
        List<CreateLiquidityPoolRequest.AssetInfo> assetInfoList;
        try {
            validateAssetInfo(input.getAssetOne());
            validateAssetInfo(input.getAssetTwo());
            validateEqualMarketCap(input.getAssetOne(), input.getAssetTwo());
            assetInfoList = order(input.getAssetOne(), input.getAssetTwo());
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // create internal LiquidityPool object to save in DB
        String liquidityPoolName = MessageFormat.format("{0}-{1}",
                assetInfoList.get(0).getName(), assetInfoList.get(1).getName());
        log.info("Inputs valid. Creating pool with name: {}", liquidityPoolName);
        LiquidityPool liquidityPool = LiquidityPool.builder()
                .liquidityPoolName(liquidityPoolName)
                .assetOneSupply(assetInfoList.get(0).getInitialSupply())
                .assetTwoSupply(assetInfoList.get(1).getInitialSupply())
                .assetOneLocalPrice(assetInfoList.get(0).getInitialPrice())
                .assetTwoLocalPrice(assetInfoList.get(1).getInitialPrice())
                .build();

        // save into dynamoDB
        try {
            dynamoDBClient.saveLiquidityPool(liquidityPool);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // return success / created / 204
        return ApiGatewayResponse.createEmptyResponse(204, context);
    }

    /**
     * Given the two assets, orders them into a list, based on the asset name. This ensures a single liquidity pool
     * for any two pair of assets (ex - Bananas-Apples would become Apples-Bananas).
     *
     * @param assetOne
     * @param assetTwo
     * @return
     */
    private List<CreateLiquidityPoolRequest.AssetInfo> order(final CreateLiquidityPoolRequest.AssetInfo assetOne,
                                                             final CreateLiquidityPoolRequest.AssetInfo assetTwo) {
        int compare = assetOne.getName().compareTo(assetTwo.getName());
        if (compare < 0) {
            return Arrays.asList(assetOne, assetTwo);
        } else if (compare > 0) {
            return Arrays.asList(assetTwo, assetOne);
        } else {
            // asset names are same, throw exception
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ASSET);
        }
    }

    /**
     * Validations on the AssetInfo.
     *
     * @param assetInfo
     */
    private void validateAssetInfo(final CreateLiquidityPoolRequest.AssetInfo assetInfo) {
        if (assetInfo == null || assetInfo.getName() == null
                || assetInfo.getInitialPrice() == null || assetInfo.getInitialSupply() == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        if (!ServiceConstants.ALLOWED_ASSETS.contains(assetInfo.getName())) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_ASSET_NAME);
        }
        if (assetInfo.getInitialPrice() < ServiceLimits.MIN_PRICE
                || assetInfo.getInitialPrice() > ServiceLimits.MAX_PRICE) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_PRICE_RANGE);
        }
        if (assetInfo.getInitialSupply() < ServiceLimits.MIN_SUPPLY
                || assetInfo.getInitialSupply() > ServiceLimits.MAX_SUPPLY) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_SUPPLY_RANGE);
        }
    }

    private void validateEqualMarketCap(final CreateLiquidityPoolRequest.AssetInfo assetOne,
                                        final CreateLiquidityPoolRequest.AssetInfo assetTwo) {
        if (assetOne.getInitialPrice() * assetOne.getInitialSupply() !=
                assetTwo.getInitialPrice() * assetTwo.getInitialSupply()) {
            throw new IllegalArgumentException(ErrorMessages.UNEQUAL_MARKET_CAP_LIQUIDITY_UPDATE);
        }
    }

    /**
     * Input request.
     */
    @Data
    public static class CreateLiquidityPoolRequest {
        private AssetInfo assetOne;
        private AssetInfo assetTwo;

        @Data
        public static class AssetInfo {
            private String name;
            private Double initialPrice;
            private Double initialSupply;
        }
    }
}

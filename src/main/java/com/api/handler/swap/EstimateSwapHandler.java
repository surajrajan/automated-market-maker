package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.jwt.JWTClient;
import com.config.ErrorMessages;
import com.config.ServiceConstants;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.model.SwapContract;
import com.serverless.ApiGatewayResponse;
import com.util.LiquidityPoolUtil;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Date;

@Slf4j
@Setter
public class EstimateSwapHandler implements RequestHandler<EstimateSwapHandler.EstimateSwapRequest, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;
    private JWTClient jwtClient;

    private static final Integer SWAP_EXPIRY_IN_SECONDS = 5;

    public EstimateSwapHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
        this.jwtClient = DaggerAppDependencies.builder().build().jwtClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(EstimateSwapRequest input, Context context) {
        log.info("Received request: {}", input);
        LiquidityPool liquidityPool;
        SwapContract swapContract;
        try {
            validateRequest(input);
            String liquidityPoolName = LiquidityPoolUtil.getLiquidityPoolName(input.getAssetNameIn(), input.getAssetNameOut());
            liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);
            LiquidityPoolUtil.logKValue(liquidityPool);
            swapContract = MarketMakerLogic.createSwapContract(liquidityPool, input);
        } catch (IllegalArgumentException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // return jwt claim token
        Date expiresAt = DateTime.now().plusSeconds(SWAP_EXPIRY_IN_SECONDS).toDate();
        String jwtClaimToken = jwtClient.getJwtClaim(swapContract, expiresAt);

        // return success response
        EstimateSwapResponse estimateSwapResponse = new EstimateSwapResponse();
        estimateSwapResponse.setSwapContract(swapContract);
        estimateSwapResponse.setSwapJwtClaim(jwtClaimToken);
        return ApiGatewayResponse.createSuccessResponse(estimateSwapResponse, context);
    }

    private void validateRequest(final EstimateSwapRequest request) {
        if (request == null || request.getAssetNameIn() == null
                || request.getAssetAmountIn() == null || request.getAssetNameOut() == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        if (!ServiceConstants.ALLOWED_ASSETS.contains(request.getAssetNameIn())) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_ASSET_NAME);
        }
        if (request.getAssetAmountIn() < 0) {
            throw new IllegalArgumentException(ErrorMessages.NEGATIVE_AMOUNT_TO_SWAP);
        }
    }

    /**
     * Input request.
     */
    @Data
    public static class EstimateSwapRequest {
        private String assetNameIn;
        private Double assetAmountIn;
        private String assetNameOut;
    }

    @Data
    public static class EstimateSwapResponse {
        private String swapJwtClaim;
        private SwapContract swapContract;
    }
}

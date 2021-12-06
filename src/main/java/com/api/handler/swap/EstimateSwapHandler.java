package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.config.ErrorMessages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.model.SwapContract;
import com.model.types.Asset;
import com.serverless.ApiGatewayResponse;
import com.util.LiquidityPoolUtil;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class EstimateSwapHandler implements RequestHandler<EstimateSwapHandler.EstimateSwapRequest, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;
    private KMSClient kmsClient;

    private static ObjectMapper objectMapper = new ObjectMapper();

    public EstimateSwapHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
        this.kmsClient = DaggerAppDependencies.builder().build().kmsClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(EstimateSwapRequest input, Context context) {
        log.info("Received request: {}", input);
        final LiquidityPool liquidityPool;
        final SwapContract swapContract;
        try {
            validateRequest(input);
            String liquidityPoolName = LiquidityPoolUtil.getLiquidityPoolName(input.getAssetNameIn(), input.getAssetNameOut());
            liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);
            swapContract = MarketMakerLogic.createSwapContract(liquidityPool, input);
        } catch (IllegalArgumentException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // return claim token
        final String encryptedClaim;
        try {
            final String swapContractAsString = objectMapper.writeValueAsString(swapContract);
            log.info("swapContractAsString: {}", swapContractAsString);
            encryptedClaim = kmsClient.encrypt(swapContractAsString);
            log.info("encryptedClaim: {}", encryptedClaim);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // return success response
        EstimateSwapResponse estimateSwapResponse = new EstimateSwapResponse();
        estimateSwapResponse.setSwapContract(swapContract);
        estimateSwapResponse.setSwapClaim(encryptedClaim);
        return ApiGatewayResponse.createSuccessResponse(estimateSwapResponse, context);
    }

    private void validateRequest(final EstimateSwapRequest request) {
        if (request == null || request.getAssetNameIn() == null
                || request.getAssetAmountIn() == null || request.getAssetNameOut() == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        if (!Asset.isValidAssetName(request.getAssetNameIn()) || !Asset.isValidAssetName(request.getAssetNameOut())) {
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
        private String swapClaim;
        private SwapContract swapContract;
    }
}

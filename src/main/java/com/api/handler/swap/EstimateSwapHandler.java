package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.config.ErrorMessages;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.model.SwapContract;
import com.model.exception.InvalidInputException;
import com.model.types.Asset;
import com.serverless.ApiGatewayResponse;
import com.util.LiquidityPoolUtil;
import com.util.ObjectMapperUtil;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
public class EstimateSwapHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;
    private KMSClient kmsClient;

    public EstimateSwapHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
        this.kmsClient = DaggerAppDependencies.builder().build().kmsClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        log.info("Received request event: {}", requestEvent);
        final LiquidityPool liquidityPool;
        final EstimateSwapRequest estimateSwapRequest;
        try {
            estimateSwapRequest = ObjectMapperUtil.toClass(requestEvent.getBody(), EstimateSwapRequest.class);
            validateRequest(estimateSwapRequest);
            String liquidityPoolName = LiquidityPoolUtil.inferLiquidityPoolFromSwapRequest(estimateSwapRequest);
            liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        SwapContract swapContract = MarketMakerLogic.createSwapContract(liquidityPool, estimateSwapRequest);

        // return claim token
        final String encryptedClaim;
        try {
            final String swapContractAsString = ObjectMapperUtil.toString(swapContract);
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

    private void validateRequest(final EstimateSwapRequest request) throws InvalidInputException {
        if (request == null || request.getAssetNameIn() == null
                || request.getAssetAmountIn() == null || request.getAssetNameOut() == null) {
            throw new InvalidInputException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        if (!Asset.isValidAssetName(request.getAssetNameIn()) || !Asset.isValidAssetName(request.getAssetNameOut())) {
            throw new InvalidInputException(ErrorMessages.INVALID_ASSET_NAME);
        }
        if (request.getAssetNameIn().equals(request.getAssetNameOut())) {
            throw new InvalidInputException(ErrorMessages.DUPLICATE_ASSET);
        }
        if (request.getAssetAmountIn() < 0) {
            throw new InvalidInputException(ErrorMessages.NEGATIVE_AMOUNT_TO_SWAP);
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

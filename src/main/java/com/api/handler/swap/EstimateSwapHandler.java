package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.api.handler.swap.model.EstimateSwapResponse;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.config.ErrorMessages;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.client.kms.token.SwapClaimToken;
import com.model.SwapEstimate;
import com.model.SwapRequest;
import com.model.exception.InvalidInputException;
import com.model.types.Asset;
import com.serverless.ApiGatewayResponse;
import com.util.LiquidityPoolUtil;
import com.util.ObjectMapperUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Date;

@Slf4j
@Setter
public class EstimateSwapHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

    private DynamoDBClient dynamoDBClient;
    private KMSClient kmsClient;
    private MarketMakerLogic marketMakerLogic;

    private static final Integer SWAP_ESTIMATE_EXPIRES_AFTER_IN_SECONDS = 90;

    public EstimateSwapHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
        this.kmsClient = DaggerAppDependencies.builder().build().kmsClient();
        this.marketMakerLogic = new MarketMakerLogic();
    }

    @Override
    public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        // validate request and load liquidity pool from swap request
        log.info("Received request event: {}", requestEvent);
        final LiquidityPool liquidityPool;
        final SwapRequest swapRequest;
        try {
            swapRequest = ObjectMapperUtil.toClass(requestEvent.getBody(), SwapRequest.class);
            validateRequest(swapRequest);
            String liquidityPoolName = LiquidityPoolUtil.inferLiquidityPoolFromSwapRequest(swapRequest);
            liquidityPool = dynamoDBClient.loadLiquidityPool(liquidityPoolName);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // create an estimate based on the swap request
        SwapEstimate swapEstimate = marketMakerLogic.createSwapEstimate(liquidityPool, swapRequest);

        // create a swap claim to "claim" / "execute" this swap later
        SwapClaimToken swapClaimToken = new SwapClaimToken();
        // set the swapContractId (determined by the requestId of this estimate invocation)
        final String swapContractId = context.getAwsRequestId();
        log.info("swapContractId: {}", swapContractId);
        Date expiresAt = DateTime.now().plusSeconds(SWAP_ESTIMATE_EXPIRES_AFTER_IN_SECONDS).toDate();
        swapClaimToken.setExpiresAt(expiresAt);
        swapClaimToken.setSwapContractId(swapContractId);
        swapClaimToken.setSwapRequest(swapRequest);

        // issue encrypted claim token, based on swapContractId
        String swapClaimTokenAsString;
        try {
            swapClaimTokenAsString = kmsClient.encrypt(swapClaimToken);
            log.info("swapClaimToken: {}", swapClaimToken);
        } catch (InvalidInputException e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // return success response
        EstimateSwapResponse estimateSwapResponse = new EstimateSwapResponse();
        estimateSwapResponse.setSwapEstimate(swapEstimate);
        estimateSwapResponse.setSwapClaimToken(swapClaimTokenAsString);
        return ApiGatewayResponse.createSuccessResponse(estimateSwapResponse, context);
    }

    private void validateRequest(final SwapRequest request) throws InvalidInputException {
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
}

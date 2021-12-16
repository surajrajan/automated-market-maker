package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.api.handler.swap.model.EstimateSwapResponse;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.client.kms.token.SwapClaimToken;
import com.config.ErrorMessages;
import com.config.ServiceConstants;
import com.logic.MarketMakerLogic;
import com.model.LiquidityPool;
import com.model.SwapEstimate;
import com.model.SwapRequest;
import com.model.exception.InvalidInputException;
import com.model.types.Asset;
import com.util.LiquidityPoolUtil;
import com.util.ObjectMapperUtil;
import com.util.ResponseUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Handler for EstimateSwap API.
 * Requires an underlying LiquidityPool to exist already.
 * Requires a SwapRequest body containing request details. Returns an object containing:
 * 1. A calculated SwapEstimate, based on the LiquidityPool price / supply details.
 * 2. A swapClaimToken String, which is a one-time claim use token to execute the SwapRequest using the SubmitSwap API.
 *    The token is an encrypted version (using KMS key) of the swap details. Every token has a uniqueId associated with
 *    it, which is the uniqueAwsRequestId. This is used to prevent a single token from being consumed multiple times.
 */
@Slf4j
@Setter
public class EstimateSwapHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private DynamoDBClient dynamoDBClient;
    private KMSClient kmsClient;
    private MarketMakerLogic marketMakerLogic;

    public EstimateSwapHandler() {
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
        this.kmsClient = DaggerAppDependencies.builder().build().kmsClient();
        this.marketMakerLogic = new MarketMakerLogic();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        // validate request and load liquidity pool from swap request
        log.info("Received request event: {}", requestEvent);
        final LiquidityPool liquidityPool;
        final SwapRequest swapRequest;
        try {
            swapRequest = ObjectMapperUtil.toClass(requestEvent.getBody(), SwapRequest.class);
            validateRequest(swapRequest);
            String poolName = LiquidityPoolUtil.inferPoolNameFromSwapRequest(swapRequest);
            liquidityPool = dynamoDBClient.loadLiquidityPool(poolName);
        } catch (InvalidInputException e) {
            return ResponseUtil.createBadRequest(e.getMessage(), context);
        }

        // create an estimate based on the swap request
        SwapEstimate swapEstimate = marketMakerLogic.createSwapEstimate(liquidityPool, swapRequest);

        // create a swap claim to "claim" / "execute" this swap later
        SwapClaimToken swapClaimToken = new SwapClaimToken();
        // set the swapContractId (determined by the requestId of this estimate invocation)
        final String swapContractId = context.getAwsRequestId();
        log.info("swapContractId: {}", swapContractId);
        Date expiresAt = DateTime.now().plusSeconds(ServiceConstants.SWAP_ESTIMATE_EXPIRES_AFTER_IN_SECONDS).toDate();
        swapClaimToken.setExpiresAt(expiresAt);
        swapClaimToken.setSwapContractId(swapContractId);
        swapClaimToken.setSwapRequest(swapRequest);

        // issue encrypted claim token, based on swapContractId
        final String swapClaimTokenAsString = ObjectMapperUtil.toString(swapClaimToken);
        final String encryptedSwapClaimToken = kmsClient.encrypt(swapClaimTokenAsString);
        log.info("swapClaimToken: {}", swapClaimToken);


        // return success response
        EstimateSwapResponse estimateSwapResponse = new EstimateSwapResponse();
        estimateSwapResponse.setSwapEstimate(swapEstimate);
        estimateSwapResponse.setSwapClaimToken(encryptedSwapClaimToken);
        return ResponseUtil.createSuccessResponse(estimateSwapResponse, context);
    }

    private void validateRequest(final SwapRequest request) throws InvalidInputException {
        if (request == null || request.getInName() == null
                || request.getInAmount() == null || request.getOutName() == null) {
            throw new InvalidInputException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        if (!Asset.isValidAssetName(request.getInName()) || !Asset.isValidAssetName(request.getOutName())) {
            throw new InvalidInputException(ErrorMessages.INVALID_ASSET_NAME);
        }
        if (request.getInName().equals(request.getOutName())) {
            throw new InvalidInputException(ErrorMessages.DUPLICATE_ASSET);
        }
        if (request.getInAmount() < 0) {
            throw new InvalidInputException(ErrorMessages.NEGATIVE_AMOUNT_TO_SWAP);
        }
    }
}

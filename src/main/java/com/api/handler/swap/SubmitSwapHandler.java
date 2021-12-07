package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.client.sqs.SQSClient;
import com.config.ErrorMessages;
import com.model.SwapClaim;
import com.model.exception.InvalidInputException;
import com.serverless.ApiGatewayResponse;
import com.util.ObjectMapperUtil;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Setter
public class SubmitSwapHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

    private SQSClient sqsClient;
    private KMSClient kmsClient;
    private DynamoDBClient dynamoDBClient;

    public SubmitSwapHandler() {
        this.sqsClient = DaggerAppDependencies.builder().build().sqsClient();
        this.kmsClient = DaggerAppDependencies.builder().build().kmsClient();
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        // get swap claim object from encrypted swap claim input
        log.info("Received request event: {}", requestEvent);
        final SubmitSwapRequest submitSwapRequest;
        final SwapClaim swapClaim;
        try {
            submitSwapRequest = ObjectMapperUtil.toClass(requestEvent.getBody(), SubmitSwapRequest.class);
            validateRequest(submitSwapRequest);
            String swapClaimAsString = kmsClient.decrypt(submitSwapRequest.getSwapClaimToken());
            swapClaim = ObjectMapperUtil.toClass(swapClaimAsString, SwapClaim.class);
            log.info("Swap claim: {}", swapClaim);
        } catch (InvalidInputException e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(ErrorMessages.INVALID_CLAIM, context);
        }

        // validate expiry
        Date now = new Date();
        if (now.after(swapClaim.getExpiresAt())) {
            log.error("Swap is expired. Current time: {}", now);
            return ApiGatewayResponse.createBadRequest(ErrorMessages.CLAIM_EXPIRED, context);
        }

        // set transactionId as unique estimate swapContractId
        String transactionId = swapClaim.getSwapContractId();
        try {
            // write to DB that transactionId has started
            dynamoDBClient.initializeTransaction(transactionId);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(ErrorMessages.CLAIM_ALREADY_USED, context);
        }

        // submit swap claim to be processed by SQS
        sqsClient.submitSwap(swapClaim);
        log.info("Submitted swap claim to SQS.");

        // return success response
        SubmitSwapResponse response = new SubmitSwapResponse();
        response.setTransactionId(transactionId);
        return ApiGatewayResponse.createSuccessResponse(response, context);
    }

    private void validateRequest(final SubmitSwapRequest request) {
        if (request == null || request.getSwapClaimToken() == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
    }

    @Data
    public static class SubmitSwapRequest {
        private String swapClaimToken;
    }

    @Data
    public static class SubmitSwapResponse {
        private String transactionId;
    }
}

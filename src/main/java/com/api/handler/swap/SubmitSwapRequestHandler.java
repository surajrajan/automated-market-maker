package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.client.sqs.SQSClient;
import com.config.ErrorMessages;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.SwapContract;
import com.model.SwapRequest;
import com.serverless.ApiGatewayResponse;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Setter
public class SubmitSwapRequestHandler implements RequestHandler<SubmitSwapRequestHandler.SubmitSwapRequest, ApiGatewayResponse> {

    private SQSClient sqsClient;
    private KMSClient kmsClient;
    private DynamoDBClient dynamoDBClient;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public SubmitSwapRequestHandler() {
        this.sqsClient = DaggerAppDependencies.builder().build().sqsClient();
        this.kmsClient = DaggerAppDependencies.builder().build().kmsClient();
        this.dynamoDBClient = DaggerAppDependencies.builder().build().dynamoDBClient();
    }

    @Override
    public ApiGatewayResponse handleRequest(SubmitSwapRequest input, Context context) {
        log.info("Received request: {}", input);
        final SwapContract swapContract;
        try {
            validateRequest(input);
            final String encryptedSwapClaim = input.getSwapClaim();
            final String swapContractAsString = kmsClient.decrypt(encryptedSwapClaim);
            log.info("swapContractAsString: {}", swapContractAsString);
            swapContract = objectMapper.readValue(swapContractAsString, SwapContract.class);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        Date now = new Date();
        if (now.after(swapContract.getExpiresAt())) {
            log.error("Swap is expired. Current time: {}", now);
            return ApiGatewayResponse.createBadRequest(ErrorMessages.CLAIM_EXPIRED, context);
        }

        // write to DB that transaction has started
        final String transactionId = dynamoDBClient.initializeTransaction();
        log.info("Initialized transactionId: {} to DB.", transactionId);

        // create request to put into SQS
        SwapRequest swapRequest = new SwapRequest();
        swapRequest.setTransactionId(transactionId);
        swapRequest.setSwapContract(swapContract);
        sqsClient.submitSwap(swapRequest);
        log.info("Submitted transactionId: {} to SQS.", transactionId);

        // return success response
        SubmitSwapResponse response = new SubmitSwapResponse();
        response.setTransactionId(transactionId);
        return ApiGatewayResponse.createSuccessResponse(response, context);
    }

    private void validateRequest(final SubmitSwapRequest request) {
        if (request == null || request.getSwapClaim() == null) {
            throw new IllegalArgumentException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
    }

    @Data
    public static class SubmitSwapRequest {
        private String swapClaim;
    }

    @Data
    public static class SubmitSwapResponse {
        private String transactionId;
    }
}

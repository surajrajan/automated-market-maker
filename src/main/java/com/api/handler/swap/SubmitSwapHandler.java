package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.client.sqs.SQSClient;
import com.config.ErrorMessages;
import com.model.SwapContract;
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
        log.info("Received request event: {}", requestEvent);
        final SubmitSwapRequest submitSwapRequest;
        final SwapContract swapContract;
        try {
            submitSwapRequest = ObjectMapperUtil.toClass(requestEvent.getBody(), SubmitSwapRequest.class);
            validateRequest(submitSwapRequest);
            final String encryptedSwapClaim = submitSwapRequest.getSwapClaim();
            // convert the encrypted payload into raw SwapContract json
            final String swapContractAsString = kmsClient.decrypt(encryptedSwapClaim);
            log.info("swapContractAsString: {}", swapContractAsString);
            swapContract = ObjectMapperUtil.toClass(swapContractAsString, SwapContract.class);
        } catch (InvalidInputException e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(ErrorMessages.INVALID_CLAIM, context);
        }

        Date now = new Date();
        if (now.after(swapContract.getExpiresAt())) {
            log.error("Swap is expired. Current time: {}", now);
            return ApiGatewayResponse.createBadRequest(ErrorMessages.CLAIM_EXPIRED, context);
        }

        // set transactionId as unique estimate swapContractId
        String transactionId = swapContract.getSwapContractId();
        try {
            // write to DB that transactionId has started
            dynamoDBClient.initializeTransaction(transactionId);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(ErrorMessages.CLAIM_ALREADY_USED, context);
        }
        log.info("Initialized transactionId: {} to DB.", transactionId);

        // create request to put into SQS. set unique contract
        sqsClient.submitSwapContract(swapContract);
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

package com.api.handler.swap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.api.handler.swap.model.SubmitSwapRequest;
import com.api.handler.swap.model.SubmitSwapResponse;
import com.client.DaggerAppDependencies;
import com.client.dynamodb.DynamoDBClient;
import com.client.kms.KMSClient;
import com.client.kms.token.SwapClaimToken;
import com.client.sqs.SQSClient;
import com.config.ErrorMessages;
import com.model.Transaction;
import com.model.exception.InvalidInputException;
import com.model.types.TransactionStatus;
import com.serverless.ApiGatewayResponse;
import com.util.ObjectMapperUtil;
import lombok.NonNull;
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
        final SwapClaimToken swapClaimToken;
        try {
            submitSwapRequest = ObjectMapperUtil.toClass(requestEvent.getBody(), SubmitSwapRequest.class);
            validateRequest(submitSwapRequest);
            swapClaimToken = kmsClient.decrypt(submitSwapRequest.getSwapClaimToken());
            log.info("swapClaimToken: {}", swapClaimToken);
        } catch (InvalidInputException e) {
            log.error(e.getMessage(), e);
            return ApiGatewayResponse.createBadRequest(e.getMessage(), context);
        }

        // validate expiry
        Date now = new Date();
        if (now.after(swapClaimToken.getExpiresAt())) {
            log.error("Swap is expired. Current time: {}", now);
            return ApiGatewayResponse.createBadRequest(ErrorMessages.CLAIM_EXPIRED, context);
        }

        // set transactionId as unique estimate swapContractId
        String transactionId = swapClaimToken.getSwapContractId();
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setTransactionState(TransactionStatus.STARTED.name());
        transaction.setTimeStarted(now);
        try {
            // write to DB that transaction has started
            dynamoDBClient.initializeTransaction(transaction);
        } catch (InvalidInputException e) {
            return ApiGatewayResponse.createBadRequest(ErrorMessages.CLAIM_ALREADY_USED, context);
        }

        // submit swap claim token to be processed by SQS
        final String swapClaimTokenAsString = ObjectMapperUtil.toString(swapClaimToken);
        sqsClient.submitMessage(swapClaimTokenAsString);
        log.info("Submitted swap claim to SQS.");

        // return success response
        SubmitSwapResponse response = new SubmitSwapResponse();
        response.setTransactionId(transactionId);
        return ApiGatewayResponse.createSuccessResponse(response, context);
    }

    private void validateRequest(@NonNull final SubmitSwapRequest request) throws InvalidInputException {
        if (request.getSwapClaimToken() == null) {
            throw new InvalidInputException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
    }
}

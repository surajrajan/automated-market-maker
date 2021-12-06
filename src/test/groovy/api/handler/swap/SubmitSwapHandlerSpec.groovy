package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.api.handler.swap.SubmitSwapRequestHandler
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.client.sqs.SQSClient
import com.config.ErrorMessages
import com.fasterxml.jackson.databind.ObjectMapper
import com.model.SwapRequest
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject

class SubmitSwapHandlerSpec extends Specification {

    def sqsClient = Mock(SQSClient)
    def dynamoDBClient = Mock(DynamoDBClient)
    def kmsClient = Mock(KMSClient)
    def context = Mock(Context)

    private final String someValidSwapClaim = "someValidSwapClaim"
    private final String someValidSwapContract = "{\"inName\":\"Apples\",\"inAssetAmount\":{\"amount\":7000.0,\"price\":100.0},\"outName\":\"Bananas\",\"outAssetAmount\":{\"amount\":6140.35087719298,\"price\":113.99999999999999},\"expiresAt\":4100223964000}"
    private final String someExpiredSwapContract = "{\"inName\":\"Apples\",\"inAssetAmount\":{\"amount\":7000.0,\"price\":100.0},\"outName\":\"Bananas\",\"outAssetAmount\":{\"amount\":6140.35087719298,\"price\":113.99999999999999},\"expiresAt\":1638774364000}"
    private final String someTransactionId = "someTransactionId"

    private SubmitSwapRequestHandler.SubmitSwapRequest submitSwapRequest
    private ObjectMapper objectMapper

    @Subject
    SubmitSwapRequestHandler submitSwapRequestHandler

    def setup() {
        submitSwapRequestHandler = new SubmitSwapRequestHandler()
        submitSwapRequestHandler.setSqsClient(sqsClient)
        submitSwapRequestHandler.setDynamoDBClient(dynamoDBClient)
        submitSwapRequestHandler.setKmsClient(kmsClient)

        submitSwapRequest = new SubmitSwapRequestHandler.SubmitSwapRequest()
        submitSwapRequest.setSwapClaim(someValidSwapClaim)

        objectMapper = new ObjectMapper()
    }

    def "given valid claim should start transaction and submit swap to sqs"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(submitSwapRequest, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaim) >> {
            return someValidSwapContract
        }

        1 * dynamoDBClient.initializeTransaction() >> {
            return someTransactionId
        }
        1 * sqsClient.submitSwap(_) >> { SwapRequest swapRequest ->
            assert objectMapper.writeValueAsString(swapRequest.getSwapContract()) == someValidSwapContract
        }
        assert response.getBody().contains(someTransactionId)
        assert response.getStatusCode() == 200
    }

    def "given expired claim should throw bad request"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(submitSwapRequest, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaim) >> {
            return someExpiredSwapContract
        }
        assert response.getBody().contains(ErrorMessages.CLAIM_EXPIRED)
        assert response.getStatusCode() == 400
    }

    def "given invalid input should throw bad request"() {
        given:
        SubmitSwapRequestHandler.SubmitSwapRequest emptyRequest = new SubmitSwapRequestHandler.SubmitSwapRequest()

        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(emptyRequest, context)

        then:
        assert response.getBody().contains(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS)
        assert response.getStatusCode() == 400
    }
}

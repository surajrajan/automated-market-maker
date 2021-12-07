package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.api.handler.swap.SubmitSwapHandler
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.client.sqs.SQSClient
import com.config.ErrorMessages
import com.model.SwapRequest
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject
import util.TestUtil

class SubmitSwapHandlerSpec extends Specification {

    def sqsClient = Mock(SQSClient)
    def dynamoDBClient = Mock(DynamoDBClient)
    def kmsClient = Mock(KMSClient)
    def context = Mock(Context)

    private final String someValidSwapClaim = "someValidSwapClaim"
    private final String someValidSwapContract = "{\"inName\":\"Apples\",\"inAssetAmount\":{\"amount\":7000.0,\"price\":100.0},\"outName\":\"Bananas\",\"outAssetAmount\":{\"amount\":6140.35087719298,\"price\":113.99999999999999},\"swapContractId\":\"someSwapContractId\",\"expiresAt\":4100223964000}"
    private final String someExpiredSwapContract = "{\"inName\":\"Apples\",\"inAssetAmount\":{\"amount\":7000.0,\"price\":100.0},\"outName\":\"Bananas\",\"outAssetAmount\":{\"amount\":6140.35087719298,\"price\":113.99999999999999},\"swapContractId\":\"someSwapContractId\",\"expiresAt\":1638774364000}"
    private final String someSwapContractId = "someSwapContractId"

    private APIGatewayProxyRequestEvent requestEvent
    private SubmitSwapHandler.SubmitSwapRequest submitSwapRequest

    @Subject
    SubmitSwapHandler submitSwapRequestHandler

    def setup() {
        submitSwapRequestHandler = new SubmitSwapHandler()
        submitSwapRequestHandler.setSqsClient(sqsClient)
        submitSwapRequestHandler.setDynamoDBClient(dynamoDBClient)
        submitSwapRequestHandler.setKmsClient(kmsClient)

        submitSwapRequest = new SubmitSwapHandler.SubmitSwapRequest()
        submitSwapRequest.setSwapClaim(someValidSwapClaim)

        requestEvent = TestUtil.createEventRequest(submitSwapRequest)
    }

    def "given valid claim should start transaction and submit swap to sqs"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaim) >> {
            return someValidSwapContract
        }

        1 * dynamoDBClient.initializeTransaction(someSwapContractId)
        1 * sqsClient.submitSwap(_) >> { SwapRequest swapRequest ->
            assert !swapRequest.getTransactionId().isEmpty()
        }
        assert response.getBody().contains(someSwapContractId)
        assert response.getStatusCode() == 200
    }

    def "given expired claim should throw bad request"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaim) >> {
            return someExpiredSwapContract
        }
        assert response.getBody().contains(ErrorMessages.CLAIM_EXPIRED)
        assert response.getStatusCode() == 400
    }

    def "given claim is invalid format should throw bad request"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaim) >> {
            return "blah"
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_CLAIM)
    }
}

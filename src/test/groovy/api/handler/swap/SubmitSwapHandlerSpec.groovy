package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.api.handler.swap.SubmitSwapHandler
import com.api.handler.swap.model.SubmitSwapRequest
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.client.sqs.SQSClient
import com.config.ErrorMessages
import com.client.kms.token.SwapClaimToken
import com.model.SwapRequest
import com.model.exception.InvalidInputException
import com.serverless.ApiGatewayResponse
import org.joda.time.DateTime
import spock.lang.Specification
import spock.lang.Subject
import util.TestUtil

class SubmitSwapHandlerSpec extends Specification {

    def sqsClient = Mock(SQSClient)
    def dynamoDBClient = Mock(DynamoDBClient)
    def kmsClient = Mock(KMSClient)
    def context = Mock(Context)

    private final String someValidSwapClaimToken = "someValidSwapClaimToken"
    private final String someSwapContractId = "someSwapContractId"

    private APIGatewayProxyRequestEvent requestEvent
    private SubmitSwapRequest submitSwapRequest
    private SwapClaimToken swapClaimToken
    private SwapRequest swapRequest

    @Subject
    SubmitSwapHandler submitSwapRequestHandler

    def setup() {
        submitSwapRequestHandler = new SubmitSwapHandler()
        submitSwapRequestHandler.setSqsClient(sqsClient)
        submitSwapRequestHandler.setDynamoDBClient(dynamoDBClient)
        submitSwapRequestHandler.setKmsClient(kmsClient)

        submitSwapRequest = new SubmitSwapRequest()
        submitSwapRequest.setSwapClaimToken(someValidSwapClaimToken)

        requestEvent = TestUtil.createEventRequest(submitSwapRequest)

        swapClaimToken = new SwapClaimToken()
        swapClaimToken.setSwapContractId(someSwapContractId)
        swapClaimToken.setSwapRequest(swapRequest)
        swapClaimToken.setExpiresAt(new DateTime().plusHours(1).toDate())
    }


    def "given valid claim should start transaction and submit swap to sqs"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return swapClaimToken
        }

        1 * dynamoDBClient.initializeTransaction(someSwapContractId)
        1 * sqsClient.submitSwap(_) >> { SwapClaimToken swapClaim ->
            assert swapClaim.getSwapContractId() == someSwapContractId
        }
        assert response.getBody().contains(someSwapContractId)
        assert response.getStatusCode() == 200
    }

    def "given expired claim should throw bad request"() {
        given:
        swapClaimToken = new SwapClaimToken()
        swapClaimToken.setSwapContractId(someSwapContractId)
        swapClaimToken.setSwapRequest(swapRequest)
        swapClaimToken.setExpiresAt(new DateTime().minusHours(1).toDate())

        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return swapClaimToken
        }
        assert response.getBody().contains(ErrorMessages.CLAIM_EXPIRED)
        assert response.getStatusCode() == 400
    }

    def "given kms client throws invalid input exception should throw bad request"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            throw new InvalidInputException();
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_CLAIM)
    }
}

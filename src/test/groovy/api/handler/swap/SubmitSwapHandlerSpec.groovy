package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.api.handler.swap.SubmitSwapHandler
import com.api.handler.swap.model.SubmitSwapRequest
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.client.sqs.SQSClient
import com.config.ErrorMessages
import com.model.SwapClaimToken
import com.model.SwapRequest
import com.serverless.ApiGatewayResponse
import com.util.ObjectMapperUtil
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
    private SwapClaimToken swapClaim
    private SwapRequest swapRequest
    private String swapClaimAsString

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

        swapClaim = new SwapClaimToken()
        swapClaim.setSwapContractId(someSwapContractId)
        swapClaim.setSwapRequest(swapRequest)
        swapClaim.setExpiresAt(new DateTime().plusHours(1).toDate())
        swapClaimAsString = ObjectMapperUtil.toString(swapClaim)
    }


    def "given valid claim should start transaction and submit swap to sqs"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return swapClaimAsString
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
        swapClaim = new SwapClaimToken()
        swapClaim.setSwapContractId(someSwapContractId)
        swapClaim.setSwapRequest(swapRequest)
        swapClaim.setExpiresAt(new DateTime().minusHours(1).toDate())
        String expiredClaimAsString = ObjectMapperUtil.toString(swapClaim)

        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return expiredClaimAsString
        }
        assert response.getBody().contains(ErrorMessages.CLAIM_EXPIRED)
        assert response.getStatusCode() == 400
    }

    def "given claim is invalid format should throw bad request"() {
        when:
        ApiGatewayResponse response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return "blah"
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_CLAIM)
    }
}

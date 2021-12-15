package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.api.handler.swap.SubmitSwapHandler
import com.api.handler.swap.model.SubmitSwapRequest
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.client.kms.token.SwapClaimToken
import com.client.sqs.SQSClient
import com.config.ErrorMessages
import com.model.SwapRequest
import com.model.Transaction
import com.model.exception.InvalidInputException
import com.model.types.TransactionStatus
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
    private APIGatewayProxyRequestEvent invalidRequest
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

        SubmitSwapRequest emptySwapRequest = new SubmitSwapRequest()
        invalidRequest = TestUtil.createEventRequest(emptySwapRequest)
    }


    def "given valid claim should start transaction and submit swap to sqs"() {
        when:
        APIGatewayProxyResponseEvent response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return swapClaimToken
        }

        1 * dynamoDBClient.initializeTransaction(_) >> { Transaction transaction ->
            assert transaction.getTransactionId() == someSwapContractId
            assert transaction.getTransactionState() == TransactionStatus.STARTED.name()
            assert transaction.getTimeStarted() != null
            assert transaction.getTimeCompleted() == null
        }
        1 * sqsClient.submitMessage(_) >> { String swapClaimAsString ->
            SwapClaimToken swapClaimToken = ObjectMapperUtil.toClass(swapClaimAsString, SwapClaimToken.class)
            assert swapClaimToken.getSwapRequest() == swapRequest
            assert swapClaimToken.getExpiresAt() != null
            assert swapClaimToken.getSwapContractId() == someSwapContractId
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
        APIGatewayProxyResponseEvent response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return swapClaimToken
        }
        assert response.getBody().contains(ErrorMessages.CLAIM_EXPIRED)
        assert response.getStatusCode() == 400
    }

    def "given kms client throws invalid input exception should throw bad request invalid claim"() {
        when:
        APIGatewayProxyResponseEvent response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            throw new InvalidInputException(ErrorMessages.INVALID_CLAIM);
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_CLAIM)
    }

    def "given dynamo invalid input exception should throw bad request claim already used"() {
        when:
        APIGatewayProxyResponseEvent response = submitSwapRequestHandler.handleRequest(requestEvent, context)

        then:
        1 * kmsClient.decrypt(someValidSwapClaimToken) >> {
            return swapClaimToken
        }
        1 * dynamoDBClient.initializeTransaction(_) >> {
            throw new InvalidInputException()
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.CLAIM_ALREADY_USED)
    }

    def "given empty claim token should throw bad request missing fields"() {
        when:
        APIGatewayProxyResponseEvent response = submitSwapRequestHandler.handleRequest(invalidRequest, context)

        then:
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS)
    }
}

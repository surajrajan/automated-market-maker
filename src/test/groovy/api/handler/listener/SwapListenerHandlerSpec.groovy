package api.handler.listener

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.api.handler.listener.SwapListenerHandler
import com.client.dynamodb.DynamoDBClient
import com.client.kms.token.SwapClaimToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.logic.MarketMakerLogic
import com.model.LiquidityPool
import com.model.SwapEstimate
import com.model.SwapRequest
import com.model.Transaction
import com.model.types.TransactionStatus
import org.joda.time.DateTime
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class SwapListenerHandlerSpec extends Specification {

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)
    def marketMakerLogic = Mock(MarketMakerLogic)

    private static Double someValidAmountToSwap = 5000
    private static String someValidAssetOne = "Apples"
    private static String someValidAssetTwo = "Bananas"
    private String someValidLiquidityPoolName = "Apples-Bananas"
    private String someValidSwapContractId = "someValidSwapContractId"
    private LiquidityPool liquidityPool
    private SwapEstimate swapEstimate
    private SwapClaimToken swapClaim
    private SwapRequest swapRequest
    private String swapClaimAsString
    private static ObjectMapper objectMapper = new ObjectMapper()

    @Subject
    SwapListenerHandler swapRequestListenerHandler

    def setup() {
        swapRequestListenerHandler = new SwapListenerHandler()
        swapRequestListenerHandler.setDynamoDBClient(dynamoDBClient)
        swapRequestListenerHandler.setMarketMakerLogic(marketMakerLogic)
        liquidityPool = new LiquidityPool()
        swapRequest = new SwapRequest()
    }

    @Unroll
    def "given valid sqs event (#type) should write to db"() {
        given:
        swapRequest.setInAmount(someValidAmountToSwap)
        swapRequest.setInName(assetOne)
        swapRequest.setOutName(assetTwo)
        SQSEvent sqsEvent = buildSQSEvent(swapRequest)

        when:
        swapRequestListenerHandler.handleRequest(sqsEvent, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return liquidityPool
        }
        1 * marketMakerLogic.createSwapEstimate(liquidityPool, swapRequest) >> {
            return swapEstimate
        }
        1 * marketMakerLogic.applySwapEstimateToPool(swapEstimate, liquidityPool) >> {
            return liquidityPool
        }
        1 * dynamoDBClient.writeTransactionAndUpdateLiquidityPool(_, liquidityPool) >> { Transaction transaction, LiquidityPool newLiquidityPool ->
            assert transaction.getTransactionId() == someValidSwapContractId
            assert transaction.getTimeCompleted() != null
            assert transaction.getTransactionState() == TransactionStatus.FINISHED.name()
        }

        where:
        type            | assetOne          | assetTwo
        "normal order"  | someValidAssetOne | someValidAssetTwo
        "reverse order" | someValidAssetTwo | someValidAssetOne
    }

    def "given invalid message should ignore()"() {
        given:
        SQSEvent invalidEvent = new SQSEvent()

        when:
        swapRequestListenerHandler.handleRequest(invalidEvent, context)

        then:
        0 * dynamoDBClient.loadLiquidityPool(_)
        0 * marketMakerLogic.createSwapEstimate(_, _)
        0 * marketMakerLogic.applySwapEstimateToPool(_, _)
        0 * dynamoDBClient.writeTransactionAndUpdateLiquidityPool(_, _)
    }

    private SQSEvent buildSQSEvent(final SwapRequest swapRequest) {
        swapClaim = new SwapClaimToken()
        swapClaim.setSwapContractId(someValidSwapContractId)
        swapClaim.setSwapRequest(swapRequest)
        swapClaim.setExpiresAt(new DateTime().plusHours(1).toDate())
        swapClaimAsString = objectMapper.writeValueAsString(swapClaim)

        SQSEvent sqsEvent = new SQSEvent()
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage()
        sqsMessage.setBody(swapClaimAsString)
        sqsEvent.setRecords(Arrays.asList(sqsMessage))
        return sqsEvent;
    }
}

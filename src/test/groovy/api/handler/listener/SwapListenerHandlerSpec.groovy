package api.handler.listener

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.api.handler.listener.SwapListenerHandler
import com.client.dynamodb.DynamoDBClient
import com.logic.MarketMakerLogic
import com.model.LiquidityPool
import com.model.SwapClaim
import com.model.SwapEstimate
import com.model.SwapRequest
import com.model.Transaction
import com.model.types.TransactionStatus
import com.util.ObjectMapperUtil
import org.joda.time.DateTime
import spock.lang.Specification
import spock.lang.Subject

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
    private SQSEvent sqsEvent
    private SwapEstimate swapEstimate
    private SwapClaim swapClaim
    private SwapRequest swapRequest
    private String swapClaimAsString

    @Subject
    SwapListenerHandler swapRequestListenerHandler

    def setup() {
        swapRequestListenerHandler = new SwapListenerHandler()
        swapRequestListenerHandler.setDynamoDBClient(dynamoDBClient)
        swapRequestListenerHandler.setMarketMakerLogic(marketMakerLogic)

        liquidityPool = new LiquidityPool()
        swapRequest = new SwapRequest()
        swapRequest.setAssetNameIn(someValidAssetOne)
        swapRequest.setAssetNameOut(someValidAssetTwo)
        swapRequest.setAssetAmountIn(someValidAmountToSwap)

        swapClaim = new SwapClaim()
        swapClaim.setSwapContractId(someValidSwapContractId)
        swapClaim.setSwapRequest(swapRequest)
        swapClaim.setExpiresAt(new DateTime().plusHours(1).toDate())
        swapClaimAsString = ObjectMapperUtil.toString(swapClaim)

        sqsEvent = new SQSEvent()
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage()
        sqsMessage.setBody(swapClaimAsString)
        sqsEvent.setRecords(Arrays.asList(sqsMessage))
    }

    def "given valid sqs event should write to db"() {
        when:
        swapRequestListenerHandler.handleRequest(sqsEvent, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return liquidityPool
        }
        1 * marketMakerLogic.createSwapEstimate(liquidityPool, swapRequest) >> {
            return swapEstimate
        }
        1 * marketMakerLogic.applySwapToPool(swapEstimate, liquidityPool) >> {
            return liquidityPool
        }
        1 * dynamoDBClient.writeTransactionAndUpdateLiquidityPool(_, liquidityPool) >> { Transaction transaction, LiquidityPool newLiquidityPool ->
            assert transaction.getTransactionId() == someValidSwapContractId
            assert transaction.getTimeCompleted() != null
            assert transaction.getTransactionState() == TransactionStatus.FINISHED.name()
        }
    }
}

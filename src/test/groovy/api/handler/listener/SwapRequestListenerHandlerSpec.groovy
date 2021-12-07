package api.handler.listener

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.api.handler.listener.SwapRequestListenerHandler
import com.client.dynamodb.DynamoDBClient
import com.model.AssetAmount
import com.model.LiquidityPool
import spock.lang.Specification
import spock.lang.Subject

class SwapRequestListenerHandlerSpec extends Specification {

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)

    private String someValidLiquidityPoolName = "Apples-Bananas"
    private Double someValidSupply = 50000
    private Double someValidPrice = 100
    private String someValidMessageBody = "{\"inName\":\"Apples\",\"inAssetAmount\":{\"amount\":7000.0,\"price\":100.0},\"outName\":\"Bananas\",\"outAssetAmount\":{\"amount\":6140.35087719298,\"price\":113.99999999999999},\"expiresAt\":1638844624533,\"swapContractId\":\"4496eb86-9189-439c-9d38-12d1c76509ca\"}";
    private LiquidityPool liquidityPool
    private SQSEvent sqsEvent

    @Subject
    SwapRequestListenerHandler swapRequestListenerHandler

    def setup() {
        swapRequestListenerHandler = new SwapRequestListenerHandler()
        swapRequestListenerHandler.setDynamoDBClient(dynamoDBClient)

        sqsEvent = new SQSEvent()
        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage()
        sqsMessage.setBody(someValidMessageBody)
        sqsEvent.setRecords(Arrays.asList(sqsMessage))

        liquidityPool = new LiquidityPool()
        AssetAmount assetAmount = new AssetAmount()
        assetAmount.setAmount(someValidSupply)
        assetAmount.setPrice(someValidPrice)
        liquidityPool.setLiquidityPoolName(someValidLiquidityPoolName)
        liquidityPool.setAssetOne(assetAmount)
        liquidityPool.setAssetTwo(assetAmount)
    }

    def "given valid sqs event should write to db"() {
        when:
        swapRequestListenerHandler.handleRequest(sqsEvent, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return liquidityPool
        }
        1 * dynamoDBClient.writeTransaction(_, _)
    }
}

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
    private String someTransactionId = "someTransactionId"
    private String someValidMessageBody = "{\n" +
            "    \"swapContract\": {\n" +
            "        \"inName\": \"Apples\",\n" +
            "        \"inAssetAmount\": {\n" +
            "            \"amount\": 7000,\n" +
            "            \"price\": 100\n" +
            "        },\n" +
            "        \"outName\": \"Bananas\",\n" +
            "        \"outAssetAmount\": {\n" +
            "            \"amount\": 6140.35087719298,\n" +
            "            \"price\": 113.99999999999999\n" +
            "        },\n" +
            "        \"expiresAt\": 1638780346658\n" +
            "    },\n" +
            "    \"transactionId\": \"c5935208-9351-41f6-8fa8-30f9ecdd4dea\"\n" +
            "}";
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

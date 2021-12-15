package api.handler.pool

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.api.handler.pool.GetLiquidityPoolHandler
import com.client.dynamodb.DynamoDBClient
import com.config.ErrorMessages
import com.model.LiquidityPool
import com.model.PriceAmount
import com.model.exception.InvalidInputException
import spock.lang.Specification
import spock.lang.Subject
import util.TestUtil

class GetLiquidityHandlerPoolSpec extends Specification {

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)
    private APIGatewayProxyRequestEvent requestEvent
    private String someValidLiquidityPoolName = "Apples-Bananas"
    private Integer someValidSupply = 100
    private Integer someValidPrice = 10
    private PriceAmount someValidPriceAmount = new PriceAmount(someValidPrice, someValidSupply)
    private LiquidityPool someValidLiquidityPool = new LiquidityPool(someValidLiquidityPoolName,
            someValidPriceAmount, someValidPriceAmount, new Date(), new Date())

    @Subject
    GetLiquidityPoolHandler getLiquidityPoolHandler

    def setup() {
        getLiquidityPoolHandler = new GetLiquidityPoolHandler()
        getLiquidityPoolHandler.setDynamoDBClient(dynamoDBClient)
        requestEvent = new APIGatewayProxyRequestEvent()
        requestEvent.setPathParameters(TestUtil.getPoolNamePathParams(someValidLiquidityPoolName))
    }

    def "given key exists should return liquidity pool"() {
        when:
        APIGatewayProxyResponseEvent response = getLiquidityPoolHandler.handleRequest(requestEvent, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return someValidLiquidityPool
        }
        assert response.getBody().contains(someValidLiquidityPoolName) == true
        assert response.getStatusCode() == 200
    }

    def "given unable to extract poolName from path param"() {
        when:
        requestEvent.setPathParameters(new HashMap<String, String>())
        APIGatewayProxyResponseEvent response = getLiquidityPoolHandler.handleRequest(requestEvent, context)

        then:
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS) == true
    }

    def "given db client throws invalid input exception should throw bad request"() {
        when:
        APIGatewayProxyResponseEvent response = getLiquidityPoolHandler.handleRequest(requestEvent, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            throw new InvalidInputException(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME)
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME) == true
    }
}

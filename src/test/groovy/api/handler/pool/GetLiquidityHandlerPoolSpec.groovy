package api.handler.pool

import com.amazonaws.services.lambda.runtime.Context
import com.api.handler.pool.GetLiquidityPoolHandler
import com.client.dynamodb.DynamoDBClient
import com.config.ErrorMessages
import com.model.LiquidityPool
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject

class GetLiquidityHandlerPoolSpec extends Specification {

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)
    private Map<String, Object> request;
    private String someValidLiquidityPoolName = "someValidLiquidityPoolName"
    private Integer someValidPrice = 10;
    private Integer someValidSupply = 100;
    private LiquidityPool someValidLiquidityPool = new LiquidityPool(someValidLiquidityPoolName,
            someValidPrice, someValidSupply, someValidPrice, someValidSupply);

    @Subject
    GetLiquidityPoolHandler getLiquidityPoolHandler

    def setup() {
        getLiquidityPoolHandler = new GetLiquidityPoolHandler()
        getLiquidityPoolHandler.setDynamoDBClient(dynamoDBClient)
        request = new HashMap<>();
        Map<String, String> pathParameters = new HashMap<>()
        pathParameters.put("liquidityPoolName", someValidLiquidityPoolName)
        request.put("pathParameters", pathParameters)
    }

    def "given key exists should return liquidity pool"() {
        when:
        ApiGatewayResponse response = getLiquidityPoolHandler.handleRequest(request, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return someValidLiquidityPool
        }
        assert response.getBody().contains(someValidLiquidityPoolName) == true
        assert response.getStatusCode() == 200
    }

    def "given invalid key should throw bad request"() {
        given:

        when:
        ApiGatewayResponse response = getLiquidityPoolHandler.handleRequest(request, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return null
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME)
    }
}

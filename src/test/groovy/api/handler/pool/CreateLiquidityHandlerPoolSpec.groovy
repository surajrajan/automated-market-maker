package api.handler.pool

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.api.handler.pool.CreateLiquidityPoolHandler
import com.api.handler.pool.model.CreateLiquidityPoolRequest
import com.client.dynamodb.DynamoDBClient
import com.config.ErrorMessages
import com.model.AssetAmount
import com.model.LiquidityPool
import com.model.exception.InvalidInputException
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll
import util.TestUtil

class CreateLiquidityHandlerPoolSpec extends Specification {

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)

    private CreateLiquidityPoolRequest request;
    private APIGatewayProxyRequestEvent requestEvent;
    private Integer someValidSupply = 100
    private Integer someValidPrice = 10
    private AssetAmount someValidAssetAmount = AssetAmount.builder()
            .amount(someValidSupply)
            .price(someValidPrice)
            .build()
    private String someValidLiquidityPoolName = "Apples-Bananas"
    @Subject
    CreateLiquidityPoolHandler createLiquidityPoolHandler

    def setup() {
        createLiquidityPoolHandler = new CreateLiquidityPoolHandler();
        createLiquidityPoolHandler.setDynamoDBClient(dynamoDBClient)
        request = new CreateLiquidityPoolRequest()
        request.setAssetAmountOne(someValidAssetAmount)
        request.setAssetAmountTwo(someValidAssetAmount)
        requestEvent = TestUtil.createEventRequest(request, someValidLiquidityPoolName)
    }

    def "given valid inputs should call dynamoDB to save"() {
        when:
        ApiGatewayResponse response = createLiquidityPoolHandler.handleRequest(requestEvent, context)

        then:
        1 * dynamoDBClient.createLiquidityPool(_) >> { LiquidityPool liquidityPool ->
            assertLiquidityPool(liquidityPool)
        }
        assert response.getStatusCode() == 201
    }

    @Unroll
    def "given invalid input (#errorType) should throw bad request"() {
        given:
        CreateLiquidityPoolRequest request = new CreateLiquidityPoolRequest()
        AssetAmount assetAmountOne = new AssetAmount()
        assetAmountOne.setPrice(price)
        assetAmountOne.setAmount(supply)
        request.setAssetAmountOne(assetAmountOne)
        request.setAssetAmountTwo(someValidAssetAmount)
        APIGatewayProxyRequestEvent requestEvent = TestUtil.createEventRequest(request, liquidityPoolName)

        when:
        ApiGatewayResponse response = createLiquidityPoolHandler.handleRequest(requestEvent, context)

        then:
        assert response.getStatusCode() == 400
        assert response.getBody().contains(expectedMessage) == true

        where:
        errorType              | liquidityPoolName | price | supply | expectedMessage
        "null value"           | null              | 10    | 10     | ErrorMessages.INVALID_REQUEST_MISSING_FIELDS
        "invalid name"         | "blah"            | 10    | 10     | ErrorMessages.INVALID_LIQUIDITY_POOL_NAME
        "invalid price"        | "Apples-Bananas"  | -100  | 100    | ErrorMessages.INVALID_PRICE_RANGE
        "invalid supply"       | "Apples-Bananas"  | 10    | -100   | ErrorMessages.INVALID_SUPPLY_RANGE
        "duplicate asset name" | "Apples-Apples"   | 10    | 100    | ErrorMessages.INVALID_LIQUIDITY_POOL_NAME
        "reverse order"        | "Bananas-Apples"  | 10    | 100    | ErrorMessages.INVALID_LIQUIDITY_POOL_NAME_ASSET_ORDER
        "unequal market cap"   | "Apples-Bananas"  | 10    | 10     | ErrorMessages.UNEQUAL_MARKET_CAP_LIQUIDITY_UPDATE
    }

    def "given db client throws invalid input exception, should throw bad request exception"() {
        when:
        ApiGatewayResponse response = createLiquidityPoolHandler.handleRequest(requestEvent, context)

        then:
        dynamoDBClient.createLiquidityPool(_) >> {
            throw new InvalidInputException(ErrorMessages.LIQUIDITY_POOL_ALREADY_EXISTS);
        }
        assert response.getStatusCode() == 400
        assert response.getBody().contains(ErrorMessages.LIQUIDITY_POOL_ALREADY_EXISTS) == true
    }

    void assertLiquidityPool(final LiquidityPool liquidityPool) {
        assert liquidityPool.getLiquidityPoolName() == someValidLiquidityPoolName
        assert liquidityPool.getAssetOne().getPrice() == someValidPrice
        assert liquidityPool.getAssetTwo().getPrice() == someValidPrice
        assert liquidityPool.getAssetOne().getAmount() == someValidSupply
        assert liquidityPool.getAssetTwo().getAmount() == someValidSupply
    }
}

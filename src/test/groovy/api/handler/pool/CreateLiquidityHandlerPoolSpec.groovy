package api.handler.pool

import com.amazonaws.services.lambda.runtime.Context
import com.api.handler.pool.CreateLiquidityPoolHandler
import com.client.dynamodb.DynamoDBClient
import com.config.ErrorMessages
import com.model.LiquidityPool
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class CreateLiquidityHandlerPoolSpec extends Specification {

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)

    private static CreateLiquidityPoolHandler.CreateLiquidityPoolRequest request;
    private Integer someValidSupply = 100
    private Integer someValidPrice = 10
    private String someValidAssetOne = "Apples"
    private String someValidAssetTwo = "Bananas"
    @Subject
    CreateLiquidityPoolHandler createLiquidityPoolHandler

    def setup() {
        createLiquidityPoolHandler = new CreateLiquidityPoolHandler();
        createLiquidityPoolHandler.setDynamoDBClient(dynamoDBClient)
        request = new CreateLiquidityPoolHandler.CreateLiquidityPoolRequest()
        CreateLiquidityPoolHandler.CreateLiquidityPoolRequest.AssetInfo assetOne
                = new CreateLiquidityPoolHandler.CreateLiquidityPoolRequest.AssetInfo()
        CreateLiquidityPoolHandler.CreateLiquidityPoolRequest.AssetInfo assetTwo
                = new CreateLiquidityPoolHandler.CreateLiquidityPoolRequest.AssetInfo()
        assetOne.setName(someValidAssetOne)
        assetOne.setInitialPrice(someValidPrice)
        assetOne.setInitialSupply(someValidSupply)
        assetTwo.setName(someValidAssetTwo)
        assetTwo.setInitialPrice(someValidPrice)
        assetTwo.setInitialSupply(someValidSupply)
        request.setAssetOne(assetOne)
        request.setAssetTwo(assetTwo)
    }

    def "given valid inputs should call dynamoDB to save"() {
        when:
        ApiGatewayResponse response = createLiquidityPoolHandler.handleRequest(request, context)

        then:
        1 * dynamoDBClient.saveLiquidityPool(_) >> { LiquidityPool liquidityPool ->
            assertLiquidityPool(liquidityPool)
        }
        assert response.getStatusCode() == 204
    }

    def "given valid inputs reverse order should call dynamoDB to save"() {
        given:
        request.getAssetOne().setName(someValidAssetTwo)
        request.getAssetTwo().setName(someValidAssetOne)

        when:
        ApiGatewayResponse response = createLiquidityPoolHandler.handleRequest(request, context)

        then:
        1 * dynamoDBClient.saveLiquidityPool(_) >> { LiquidityPool liquidityPool ->
            assertLiquidityPool(liquidityPool)
        }
        assert response.getStatusCode() == 204
    }

    @Unroll
    def "given invalid input #errorType should throw bad request"() {
        given:
        request.getAssetOne().setName(assetNameOne)
        request.getAssetTwo().setName(assetNameOne)
        request.getAssetOne().setInitialPrice(price)
        request.getAssetOne().setInitialSupply(supply)

        when:
        ApiGatewayResponse response = createLiquidityPoolHandler.handleRequest(request, context)

        then:
        assert response.getStatusCode() == 400
        assert response.getBody().contains(expectedMessage) == true

        where:
        errorType              | assetNameOne | assetNameTwo | price | supply | expectedMessage
        "null value"           | null         | "Bananas"    | 10    | 10     | ErrorMessages.INVALID_REQUEST_MISSING_FIELDS
        "invalid name"         | "blah"       | "blah "      | 10    | 10     | ErrorMessages.INVALID_ASSET_NAME
        "invalid price"        | "Apples"     | "Bananas"    | -100  | 100    | ErrorMessages.INVALID_PRICE_RANGE
        "invalid supply"       | "Apples"     | "Bananas"    | 10    | -100   | ErrorMessages.INVALID_SUPPLY_RANGE
        "duplicate asset name" | "Apples"     | "Apples"     | 10    | 100    | ErrorMessages.DUPLICATE_ASSET
        "unequal market cap"   | "Apples"     | "Bananas"    | 10    | 10     | ErrorMessages.UNEQUAL_MARKET_CAP_LIQUIDITY_UPDATE
    }

    def "given db client throws illegal argument exception, should throw bad request exception"() {
        when:
        ApiGatewayResponse response = createLiquidityPoolHandler.handleRequest(request, context)

        then:
        dynamoDBClient.saveLiquidityPool(_) >> {
            throw new IllegalArgumentException();
        }
        assert response.getStatusCode() == 400
    }

    void assertLiquidityPool(final LiquidityPool liquidityPool) {
        assert liquidityPool.getLiquidityPoolName() == (someValidAssetOne + "-" + someValidAssetTwo)
        assert liquidityPool.getAssetOneLocalPrice() == someValidPrice
        assert liquidityPool.getAssetTwoLocalPrice() == someValidPrice
        assert liquidityPool.getAssetOneSupply() == someValidSupply
        assert liquidityPool.getAssetTwoSupply() == someValidSupply
    }
}

package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.api.handler.swap.EstimateSwapHandler
import com.client.dynamodb.DynamoDBClient
import com.client.jwt.JWTClient
import com.config.ErrorMessages
import com.fasterxml.jackson.databind.ObjectMapper
import com.model.LiquidityPool
import com.model.SwapContract
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject

class EstimateSwapHandlerSpec extends Specification {

    private static Double someValidSupply = 50000
    private static Double someValidPrice = 100
    private static Double someValidAmountToSwap = 5000
    private static String someValidAssetOne = "Apples"
    private static String someValidAssetTwo = "Bananas"
    private static String someValidJwtClaim = "someValidJwtClaim"
    private static String someValidLiquidityPoolName = "Apples-Bananas"

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)
    def jwtClient = Mock(JWTClient)

    def objectMapper = new ObjectMapper()

    @Subject
    EstimateSwapHandler estimateSwapHandler

    EstimateSwapHandler.EstimateSwapRequest request;
    LiquidityPool liquidityPool;

    def setup() {
        estimateSwapHandler = new EstimateSwapHandler();
        estimateSwapHandler.setDynamoDBClient(dynamoDBClient);
        estimateSwapHandler.setJwtClient(jwtClient)

        request = new EstimateSwapHandler.EstimateSwapRequest();
        request.setAssetNameIn(someValidAssetOne)
        request.setAssetNameOut(someValidAssetTwo)
        request.setAssetAmountIn(someValidAmountToSwap)

        liquidityPool = new LiquidityPool();
        liquidityPool.setLiquidityPoolName(someValidLiquidityPoolName)
        liquidityPool.setAssetOneSupply(someValidSupply)
        liquidityPool.setAssetOneLocalPrice(someValidPrice)
        liquidityPool.setAssetTwoSupply(someValidSupply)
        liquidityPool.setAssetTwoLocalPrice(someValidPrice)
    }

    def "given valid request (#type) should return valid swap contract"() {
        given:
        request.setAssetNameIn(inAsset)
        request.setAssetNameOut(outAsset)

        when:
        ApiGatewayResponse response = estimateSwapHandler.handleRequest(request, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return liquidityPool
        }
        1 * jwtClient.getJwtClaim(_, _) >> {
            return someValidJwtClaim
        }
        assert response.getStatusCode() == 200
        assert response.getBody().contains(someValidJwtClaim)
        final EstimateSwapHandler.EstimateSwapResponse estimateSwapResponse = toResponse(response.getBody())
        final SwapContract swapContract = estimateSwapResponse.getSwapContract()
        assert swapContract.getAssetNameIn() == inAsset
        assert swapContract.getAssetNameOut() == outAsset
        validateSwapContract(swapContract)

        where:
        type            | inAsset           | outAsset
        "normal order"  | someValidAssetOne | someValidAssetTwo
        "reverse order" | someValidAssetTwo | someValidAssetOne
    }

    EstimateSwapHandler.EstimateSwapResponse toResponse(final String body) {
        return objectMapper.readValue(body, EstimateSwapHandler.EstimateSwapResponse.class)
    }

    def "given invalid request (#type) should throw bad request"() {
        given:
        request.setAssetNameIn(inAsset)
        request.setAssetNameOut(outAsset)
        request.setAssetAmountIn(amountToSwap)

        when:
        ApiGatewayResponse response = estimateSwapHandler.handleRequest(request, context)

        then:
        assert response.getStatusCode() == 400
        assert response.getBody().contains(expectedErrorMessage)

        where:
        type                  | inAsset           | outAsset          | amountToSwap          | expectedErrorMessage
        "missing fields"      | null              | someValidAssetOne | someValidAmountToSwap | ErrorMessages.INVALID_REQUEST_MISSING_FIELDS
        "invalid asset"       | "blah"            | someValidAssetOne | someValidAmountToSwap | ErrorMessages.INVALID_ASSET_NAME
        "duplicate asset"     | someValidAssetOne | someValidAssetOne | someValidAmountToSwap | ErrorMessages.DUPLICATE_ASSET
        "invalid swap amount" | someValidAssetOne | someValidAssetTwo | -5                    | ErrorMessages.NEGATIVE_AMOUNT_TO_SWAP
    }

    /**
     * Validates that the swap contract has correct in / out details, along with approximately equal in / out value.
     * @param swapContract
     */
    private void validateSwapContract(final SwapContract swapContract) {
        assert swapContract.getAssetAmountIn() == someValidAmountToSwap
        assert swapContract.getAssetPriceIn() == someValidPrice
        final inValue = someValidAmountToSwap * someValidPrice
        final outValue = swapContract.getAssetPriceOut() * swapContract.getAssetAmountOut()
        assert Math.abs(inValue - outValue) < 0.01
    }
}

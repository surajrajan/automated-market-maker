package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.api.handler.swap.EstimateSwapHandler
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.config.ErrorMessages
import com.fasterxml.jackson.databind.ObjectMapper
import com.logic.MarketMakerLogic
import com.model.LiquidityPool
import com.model.SwapEstimate
import com.model.SwapRequest
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll
import util.TestUtil

class EstimateSwapHandlerSpec extends Specification {

    private static Double someValidAmountToSwap = 5000
    private static String someValidAssetOne = "Apples"
    private static String someValidAssetTwo = "Bananas"
    private static String someValidLiquidityPoolName = "Apples-Bananas"
    private static String someSwapClaimToken = "someSwapClaimToken"
    private static String someAwsRequestId = "someAwsRequestId"

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)
    def kmsClient = Mock(KMSClient)
    def marketMakerLogic = Mock(MarketMakerLogic)

    private ObjectMapper objectMapper = new ObjectMapper()

    @Subject
    EstimateSwapHandler EstimateSwapHandler

    APIGatewayProxyRequestEvent requestEvent
    SwapRequest request
    LiquidityPool liquidityPool
    SwapEstimate swapEstimate

    def setup() {
        estimateSwapHandler = new EstimateSwapHandler()
        estimateSwapHandler.setDynamoDBClient(dynamoDBClient)
        estimateSwapHandler.setKmsClient(kmsClient)
        estimateSwapHandler.setMarketMakerLogic(marketMakerLogic)

        liquidityPool = new LiquidityPool()
        liquidityPool.setLiquidityPoolName(someValidLiquidityPoolName)

        swapEstimate = new SwapEstimate()
        context.getAwsRequestId() >> someAwsRequestId
    }

    @Unroll
    def "given valid request (#type) should return valid swap contract"() {
        given:
        request = new SwapRequest()
        request.setAssetNameIn(inAsset)
        request.setAssetNameOut(outAsset)
        request.setAssetAmountIn(someValidAmountToSwap)
        requestEvent = TestUtil.createEventRequest(request, someValidLiquidityPoolName)

        when:
        ApiGatewayResponse response = estimateSwapHandler.handleRequest(requestEvent, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return liquidityPool
        }
        1 * kmsClient.encrypt(_) >> {
            return someSwapClaimToken
        }
        1 * marketMakerLogic.createSwapEstimate(liquidityPool, _) >> {
            return swapEstimate
        }
        assert response.getStatusCode() == 200
        final EstimateSwapHandler.EstimateSwapResponse estimateSwapResponse
                = objectMapper.readValue(response.getBody(), EstimateSwapHandler.EstimateSwapResponse.class)
        assert estimateSwapResponse.getSwapEstimate() == swapEstimate
        assert estimateSwapResponse.getSwapClaimToken() == someSwapClaimToken

        where:
        type            | inAsset           | outAsset
        "normal order"  | someValidAssetOne | someValidAssetTwo
        "reverse order" | someValidAssetTwo | someValidAssetOne
    }

    def "given invalid request (#type) should throw bad request"() {
        given:
        request = new SwapRequest()
        request.setAssetNameIn(inAsset)
        request.setAssetNameOut(outAsset)
        request.setAssetAmountIn(amountToSwap)
        requestEvent = TestUtil.createEventRequest(request, someValidLiquidityPoolName)

        when:
        ApiGatewayResponse response = estimateSwapHandler.handleRequest(requestEvent, context)

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
     * @param swapEstimate
     */
    private void validateSwapEstimate(final SwapEstimate swapEstimate) {
        assert swapEstimate.getInAssetAmount().getAmount() == someValidAmountToSwap
        assert swapEstimate.getInAssetAmount().getPrice() == someValidPrice
        final inValue = someValidAmountToSwap * someValidPrice
        final outValue = swapEstimate.getOutAssetAmount().getPrice() * swapEstimate.getOutAssetAmount().getAmount()
        assert Math.abs(inValue - outValue) < 0.01
    }
}

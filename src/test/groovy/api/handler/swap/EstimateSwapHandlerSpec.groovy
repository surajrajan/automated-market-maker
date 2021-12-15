package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.api.handler.swap.EstimateSwapHandler
import com.api.handler.swap.model.EstimateSwapResponse
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.client.kms.token.SwapClaimToken
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
        liquidityPool.setPoolName(someValidLiquidityPoolName)

        swapEstimate = new SwapEstimate()
        context.getAwsRequestId() >> someAwsRequestId
    }

    @Unroll
    def "given valid request (#type) should return valid swap contract"() {
        given:
        request = new SwapRequest()
        request.setInName(inAsset)
        request.setOutName(outAsset)
        request.setInAmount(someValidAmountToSwap)
        requestEvent = TestUtil.createEventRequest(request, someValidLiquidityPoolName)

        when:
        ApiGatewayResponse response = estimateSwapHandler.handleRequest(requestEvent, context)

        then:
        1 * dynamoDBClient.loadLiquidityPool(someValidLiquidityPoolName) >> {
            return liquidityPool
        }
        1 * kmsClient.encrypt(_) >> { String swapClaimAsString ->
            final SwapClaimToken swapClaimToken = objectMapper.readValue(swapClaimAsString, SwapClaimToken.class)
            assert swapClaimToken.getSwapRequest() == request
            assert swapClaimToken.getSwapContractId() == someAwsRequestId
            assert swapClaimToken.getExpiresAt() != null
            return someSwapClaimToken
        }
        1 * marketMakerLogic.createSwapEstimate(liquidityPool, _) >> {
            return swapEstimate
        }
        assert response.getStatusCode() == 200
        final EstimateSwapResponse estimateSwapResponse
                = objectMapper.readValue(response.getBody(), EstimateSwapResponse.class)
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
        request.setInName(inAsset)
        request.setOutName(outAsset)
        request.setInAmount(amountToSwap)
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
}

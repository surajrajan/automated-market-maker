package api.handler.swap

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.api.handler.swap.EstimateSwapHandler
import com.client.dynamodb.DynamoDBClient
import com.client.kms.KMSClient
import com.config.ErrorMessages
import com.fasterxml.jackson.databind.ObjectMapper
import com.model.AssetAmount
import com.model.swap.EstimateSwapRequest
import com.model.swap.EstimateSwapResponse
import com.model.LiquidityPool
import com.model.SwapContract
import com.serverless.ApiGatewayResponse
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll
import util.TestUtil

class EstimateSwapHandlerSpec extends Specification {

    private static Double someValidSupply = 50000
    private static Double someValidPrice = 100
    private static Double someValidAmountToSwap = 5000
    private static String someValidAssetOne = "Apples"
    private static String someValidAssetTwo = "Bananas"
    private static String someValidEncryptedClaim = "someValidEncryptedClaim"
    private static String someValidLiquidityPoolName = "Apples-Bananas"
    private static String someAwsRequestId = "someAwsRequestId"

    def context = Mock(Context)
    def dynamoDBClient = Mock(DynamoDBClient)
    def kmsClient = Mock(KMSClient)

    def objectMapper = new ObjectMapper()

    @Subject
            EstimateSwapHandler

    APIGatewayProxyRequestEvent requestEvent
    EstimateSwapRequest request;
    LiquidityPool liquidityPool

    def setup() {
        estimateSwapHandler = new EstimateSwapHandler()
        estimateSwapHandler.setDynamoDBClient(dynamoDBClient)
        estimateSwapHandler.setKmsClient(kmsClient)

        liquidityPool = new LiquidityPool()
        liquidityPool.setLiquidityPoolName(someValidLiquidityPoolName)
        AssetAmount someValidAssetAmount = new AssetAmount()
        someValidAssetAmount.setAmount(someValidSupply)
        someValidAssetAmount.setPrice(someValidPrice)
        liquidityPool.setAssetOne(someValidAssetAmount)
        liquidityPool.setAssetTwo(someValidAssetAmount)

        context.getAwsRequestId() >> someAwsRequestId
    }

    @Unroll
    def "given valid request (#type) should return valid swap contract"() {
        given:
        request = new EstimateSwapRequest()
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
            return someValidEncryptedClaim
        }
        assert response.getStatusCode() == 200
        assert response.getBody().contains(someValidEncryptedClaim)
        final EstimateSwapResponse estimateSwapResponse = toResponse(response.getBody())
        final SwapContract swapContract = estimateSwapResponse.getSwapContract()
        assert swapContract.getInName() == inAsset
        assert swapContract.getOutName() == outAsset
        validateSwapContract(swapContract)
        assert swapContract.getSwapContractId() == someAwsRequestId

        where:
        type            | inAsset           | outAsset
        "normal order"  | someValidAssetOne | someValidAssetTwo
        "reverse order" | someValidAssetTwo | someValidAssetOne
    }

    EstimateSwapResponse toResponse(final String body) {
        return objectMapper.readValue(body, EstimateSwapResponse.class)
    }

    def "given invalid request (#type) should throw bad request"() {
        given:
        request = new EstimateSwapRequest()
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
     * @param swapContract
     */
    private void validateSwapContract(final SwapContract swapContract) {
        assert swapContract.getInAssetAmount().getAmount() == someValidAmountToSwap
        assert swapContract.getInAssetAmount().getPrice() == someValidPrice
        final inValue = someValidAmountToSwap * someValidPrice
        final outValue = swapContract.getOutAssetAmount().getPrice() * swapContract.getOutAssetAmount().getAmount()
        assert Math.abs(inValue - outValue) < 0.01
    }
}

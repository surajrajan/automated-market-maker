package logic

import com.logic.MarketMakerLogic
import com.model.LiquidityPool
import com.model.PriceAmount
import com.model.SwapEstimate
import com.model.SwapRequest
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class MarketMakerLogicSpec extends Specification {

    private static Double someValidPrice = 100
    private static Double someValidSupply = 50000
    private static String someValidLiquidityPoolName = "Apples-Bananas"
    private static String someValidAssetOne = "Apples"
    private static String someValidAssetTwo = "Bananas"
    private static Double someValidAmountToSwap = 7000

    private LiquidityPool liquidityPool
    private SwapRequest swapRequest
    private PriceAmount someValidPriceAmount

    @Subject
    MarketMakerLogic marketMakerLogic

    def setup() {
        marketMakerLogic = new MarketMakerLogic()

        liquidityPool = new LiquidityPool()
        liquidityPool.setPoolName(someValidLiquidityPoolName)
        someValidPriceAmount = new PriceAmount(someValidPrice, someValidSupply)
        liquidityPool.setAssetOne(someValidPriceAmount)
        liquidityPool.setAssetTwo(someValidPriceAmount)
    }

    @Unroll
    def "given swap request and liquidity pool (#type), should create valid swap estimate and new liquidity pool"() {
        given:
        swapRequest = new SwapRequest()
        swapRequest.setInAmount(someValidAmountToSwap)
        swapRequest.setInName(inAsset)
        swapRequest.setOutName(outAsset)

        when:
        SwapEstimate swapEstimate = marketMakerLogic.createSwapEstimate(liquidityPool, swapRequest)
        LiquidityPool newLiquidityPool = marketMakerLogic.applySwapEstimateToPool(swapEstimate, liquidityPool)
        validateLiquidityPool(liquidityPool)
        validateLiquidityPool(newLiquidityPool)

        then:
        assert swapEstimate.getInName() == inAsset
        assert swapEstimate.getOutName() == outAsset
        validateSwapEstimate(swapEstimate)
        validateLiquidityPool(liquidityPool)
        validateLiquidityPool(newLiquidityPool)

        where:
        type            | inAsset           | outAsset
        "normal order"  | someValidAssetOne | someValidAssetTwo
        "reverse order" | someValidAssetTwo | someValidAssetOne
    }

    /**
     * Validates that the swap contract has correct in / out details, along with approximately equal in / out value.
     * @param swapEstimate
     */
    private void validateSwapEstimate(final SwapEstimate swapEstimate) {
        assert swapEstimate.getInPriceAmount().getAmount() == someValidAmountToSwap
        assert swapEstimate.getInPriceAmount().getPrice() == someValidPrice
        final inValue = someValidAmountToSwap * someValidPrice
        final outValue = swapEstimate.getOutPriceAmount().getPrice() * swapEstimate.getOutPriceAmount().getAmount()
        assert Math.abs(inValue - outValue) < 0.01
    }

    /**
     * Validates that both the market caps of an asset in a liquidity pool are approximately the same.
     * @param swapEstimate
     */
    private void validateLiquidityPool(final LiquidityPool liquidityPool) {
        assert liquidityPool.getPoolName() == someValidLiquidityPoolName
        Double marketCapAssetOne = liquidityPool.getAssetOne().getPrice() * liquidityPool.getAssetOne().getAmount()
        Double marketCapAssetTwo = liquidityPool.getAssetTwo().getPrice() * liquidityPool.getAssetTwo().getAmount()
        assert Math.abs(marketCapAssetOne - marketCapAssetTwo) < 0.01
    }
}

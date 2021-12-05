package com.logic;

import com.api.handler.swap.EstimateSwapHandler;
import com.model.AssetAmount;
import com.model.LiquidityPool;
import com.model.SwapContract;
import com.util.LiquidityPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;

@Slf4j
public class MarketMakerLogic {

    private static final Integer SWAP_EXPIRY_IN_SECONDS = 90;

    /**
     * Given an ordered list of .
     *
     * @param swapContract
     * @param liquidityPool
     */
    public static LiquidityPool applySwapToPool(final SwapContract swapContract,
                                                final LiquidityPool liquidityPool) {

        // create a new pool with same new
        final LiquidityPool newLiquidityPool = new LiquidityPool();
        newLiquidityPool.setLiquidityPoolName(liquidityPool.getLiquidityPoolName());

        // logic
        final Double constantMarketCapOne = liquidityPool.getAssetOne().getAmount() * liquidityPool.getAssetOne().getPrice();
        final String assetNames[] = liquidityPool.getLiquidityPoolName().split("-");
        final String assetNameOne = assetNames[0];

        AssetAmount newAssetAmountOne = new AssetAmount();
        AssetAmount newAssetAmountTwo = new AssetAmount();

        // depending on order, swap the supply
        if (swapContract.getAssetNameIn().equals(assetNameOne)) {
            newAssetAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() + swapContract.getAssetAmountIn());
            newAssetAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() - swapContract.getAssetAmountOut());
        } else {
            newAssetAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() - swapContract.getAssetAmountOut());
            newAssetAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() + swapContract.getAssetAmountIn());
        }
        newAssetAmountOne.setPrice(constantMarketCapOne / liquidityPool.getAssetOne().getAmount());
        newAssetAmountTwo.setPrice(constantMarketCapOne / liquidityPool.getAssetTwo().getAmount());
        newLiquidityPool.setAssetOne(newAssetAmountOne);
        newLiquidityPool.setAssetTwo(newAssetAmountTwo);
        return newLiquidityPool;
    }

    public static SwapContract createSwapContract(final LiquidityPool liquidityPool,
                                                  final EstimateSwapHandler.EstimateSwapRequest request) {
        // create map to access assetName to assetInfo
        Map<String, AssetAmount> assetAmountList = LiquidityPoolUtil.getAssetNameAssetAmountMap(liquidityPool);

        // calculate details about asset being swapped in / out
        AssetAmount assetInInfo = assetAmountList.get(request.getAssetNameIn());
        AssetAmount assetOutInfo = assetAmountList.get(request.getAssetNameOut());
        Double inSupply = assetInInfo.getAmount();
        Double inPrice = assetInInfo.getPrice();
        Double outSupply = assetOutInfo.getAmount();
        Double outPrice = assetOutInfo.getPrice();
        log.info("inSupply: {}, inPrice: {}, outSupply: {}, outPrice: {}",
                inSupply, inPrice, outSupply, outPrice);

        // calculate amount returned
        Double constantMarketCap = inSupply * inPrice;
        Double k = constantMarketCap * constantMarketCap;
        Double marketCapBeingSwappedIn = request.getAssetAmountIn() * inPrice;
        Double newMarketCapIn = constantMarketCap + marketCapBeingSwappedIn;
        Double newMarketCapOut = k / newMarketCapIn;
        Double marketCapChange = constantMarketCap - newMarketCapOut;
        Double assetAmountOut = marketCapChange / outPrice;
        Double assetOutNewPrice = constantMarketCap / newMarketCapOut * outPrice;
        log.info("marketCapBeingSwappedIn: {}, newMarketCapIn: {}, newMarketCapOut: {}, assetAmountOut: {}, assetOutNewPrice: {}",
                marketCapBeingSwappedIn, newMarketCapIn, newMarketCapOut, assetAmountOut, assetOutNewPrice);

        Date expiresAt = DateTime.now().plusSeconds(SWAP_EXPIRY_IN_SECONDS).toDate();
        SwapContract swapContract = SwapContract.builder()
                .assetNameIn(request.getAssetNameIn())
                .assetAmountIn(request.getAssetAmountIn())
                .assetPriceIn(inPrice)
                .assetNameOut(request.getAssetNameOut())
                .assetAmountOut(assetAmountOut)
                .assetPriceOut(assetOutNewPrice)
                .expiresAt(expiresAt)
                .build();
        return swapContract;
    }
}

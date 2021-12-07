package com.logic;

import com.model.AssetAmount;
import com.model.LiquidityPool;
import com.model.SwapEstimate;
import com.model.SwapRequest;
import com.util.LiquidityPoolUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MarketMakerLogic {


    /**
     * Given an ordered list of .
     *
     * @param swapEstimate
     * @param liquidityPool
     */
    public LiquidityPool applySwapToPool(final SwapEstimate swapEstimate,
                                         final LiquidityPool liquidityPool) {
        // create a new pool to return
        final LiquidityPool newLiquidityPool = new LiquidityPool();
        newLiquidityPool.setLiquidityPoolName(liquidityPool.getLiquidityPoolName());

        // logic
        final Double constantMarketCapOne = liquidityPool.getAssetOne().getAmount() * liquidityPool.getAssetOne().getPrice();
        final String assetNames[] = liquidityPool.getLiquidityPoolName().split("-");
        final String assetNameOne = assetNames[0];

        AssetAmount newAssetAmountOne = new AssetAmount();
        AssetAmount newAssetAmountTwo = new AssetAmount();

        AssetAmount inAssetAmount = swapEstimate.getInAssetAmount();
        AssetAmount outAssetAmount = swapEstimate.getOutAssetAmount();

        // depending on order, swap the supply
        if (swapEstimate.getInName().equals(assetNameOne)) {
            newAssetAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() + inAssetAmount.getAmount());
            newAssetAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() - outAssetAmount.getAmount());
        } else {
            newAssetAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() - outAssetAmount.getAmount());
            newAssetAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() + inAssetAmount.getAmount());
        }
        newAssetAmountOne.setPrice(constantMarketCapOne / newAssetAmountOne.getAmount());
        newAssetAmountTwo.setPrice(constantMarketCapOne / newAssetAmountTwo.getAmount());
        newLiquidityPool.setAssetOne(newAssetAmountOne);
        newLiquidityPool.setAssetTwo(newAssetAmountTwo);
        return newLiquidityPool;
    }

    public SwapEstimate createSwapEstimate(final LiquidityPool liquidityPool,
                                           final SwapRequest request) {
        // create map to access assetName to assetInfo to get details about asset being swapped in / out
        Map<String, AssetAmount> assetAmountList = LiquidityPoolUtil.getAssetNameAssetAmountMap(liquidityPool);
        AssetAmount assetInInfo = assetAmountList.get(request.getAssetNameIn());
        AssetAmount assetOutInfo = assetAmountList.get(request.getAssetNameOut());
        log.info("assetInInfo: {}, assetOutInfo: {}, outSupply: {}, outPrice: {}",
                assetInInfo, assetOutInfo);

        // calculate constant k to maintain
        Double constantMarketCap = assetInInfo.getAmount() * assetInInfo.getPrice();
        Double k = constantMarketCap * constantMarketCap;

        // calculate market cap being added
        Double marketCapBeingSwappedIn = request.getAssetAmountIn() * assetInInfo.getPrice();
        Double newMarketCapIn = constantMarketCap + marketCapBeingSwappedIn;

        // use constant k to determine AssetAmount out
        Double newMarketCapOut = k / newMarketCapIn;
        Double marketCapChange = constantMarketCap - newMarketCapOut;
        Double assetAmountOut = marketCapChange / assetOutInfo.getPrice();
        Double assetOutNewPrice = constantMarketCap / newMarketCapOut * assetOutInfo.getPrice();

        SwapEstimate swapEstimate = SwapEstimate.builder()
                .inName(request.getAssetNameIn())
                .inAssetAmount(AssetAmount.builder()
                        .amount(request.getAssetAmountIn())
                        .price(assetInInfo.getPrice())
                        .build())
                .outName(request.getAssetNameOut())
                .outAssetAmount(AssetAmount.builder()
                        .amount(assetAmountOut)
                        .price(assetOutNewPrice)
                        .build())
                .build();
        log.info("Created swap estimate: {}", swapEstimate);
        return swapEstimate;
    }
}

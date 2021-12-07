package com.logic;

import com.model.AssetAmount;
import com.model.LiquidityPool;
import com.model.SwapEstimate;
import com.model.SwapRequest;
import com.util.LiquidityPoolUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Class for business logic of automated market maker logic.
 */
@Slf4j
public class MarketMakerLogic {


    /**
     * Given an ordered list of .
     *
     * @param swapEstimate
     * @param liquidityPool
     */
    public LiquidityPool applySwapEstimateToPool(@NonNull final SwapEstimate swapEstimate,
                                                 @NonNull final LiquidityPool liquidityPool) {
        // create a new pool to return
        final LiquidityPool newLiquidityPool = new LiquidityPool();
        newLiquidityPool.setLiquidityPoolName(liquidityPool.getLiquidityPoolName());
        AssetAmount newAssetAmountOne = new AssetAmount();
        AssetAmount newAssetAmountTwo = new AssetAmount();

        // create map to access assetName to assetInfo to get details about asset being swapped in / out
        Map<String, AssetAmount> nameToAssetAmountMap = LiquidityPoolUtil.getAssetNameAssetAmountMap(liquidityPool);
        String assetNameOne = nameToAssetAmountMap.entrySet().iterator().next().getKey();
        // depending on order, make the swap in amount / supply
        if (swapEstimate.getInName().equals(assetNameOne)) {
            newAssetAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() + swapEstimate.getInAssetAmount().getAmount());
            newAssetAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() - swapEstimate.getOutAssetAmount().getAmount());
        } else {
            newAssetAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() - swapEstimate.getInAssetAmount().getAmount());
            newAssetAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() + swapEstimate.getInAssetAmount().getAmount());
        }

        // maintain market cap and calculate the new expected prices
        final Double constantMarketCapOne = liquidityPool.getAssetOne().getAmount() * liquidityPool.getAssetOne().getPrice();
        newAssetAmountOne.setPrice(constantMarketCapOne / newAssetAmountOne.getAmount());
        newAssetAmountTwo.setPrice(constantMarketCapOne / newAssetAmountTwo.getAmount());
        newLiquidityPool.setAssetOne(newAssetAmountOne);
        newLiquidityPool.setAssetTwo(newAssetAmountTwo);
        return newLiquidityPool;
    }

    /**
     * Given a LiquidityPool and SwapRequest, constructs a SwapEstimate.
     *
     * @param liquidityPool
     * @param swapRequest
     * @return
     */
    public SwapEstimate createSwapEstimate(@NonNull final LiquidityPool liquidityPool,
                                           @NonNull final SwapRequest swapRequest) {
        // create map to access assetName to assetInfo to get details about asset being swapped in / out
        Map<String, AssetAmount> assetAmountList = LiquidityPoolUtil.getAssetNameAssetAmountMap(liquidityPool);
        AssetAmount assetInInfo = assetAmountList.get(swapRequest.getAssetNameIn());
        AssetAmount assetOutInfo = assetAmountList.get(swapRequest.getAssetNameOut());
        log.info("assetInInfo: {}, assetOutInfo: {}, outSupply: {}, outPrice: {}",
                assetInInfo, assetOutInfo);

        // calculate constant k to maintain
        Double constantMarketCap = assetInInfo.getAmount() * assetInInfo.getPrice();
        Double k = constantMarketCap * constantMarketCap;

        // calculate market cap being added
        Double marketCapBeingSwappedIn = swapRequest.getAssetAmountIn() * assetInInfo.getPrice();
        Double newMarketCapIn = constantMarketCap + marketCapBeingSwappedIn;

        // use constant k to determine AssetAmount out
        Double newMarketCapOut = k / newMarketCapIn;
        Double marketCapChange = constantMarketCap - newMarketCapOut;
        Double assetAmountOut = marketCapChange / assetOutInfo.getPrice();
        Double assetOutNewPrice = constantMarketCap / newMarketCapOut * assetOutInfo.getPrice();

        SwapEstimate swapEstimate = SwapEstimate.builder()
                .inName(swapRequest.getAssetNameIn())
                .inAssetAmount(AssetAmount.builder()
                        .amount(swapRequest.getAssetAmountIn())
                        .price(assetInInfo.getPrice())
                        .build())
                .outName(swapRequest.getAssetNameOut())
                .outAssetAmount(AssetAmount.builder()
                        .amount(assetAmountOut)
                        .price(assetOutNewPrice)
                        .build())
                .build();
        log.info("Created swap estimate: {}", swapEstimate);
        return swapEstimate;
    }
}

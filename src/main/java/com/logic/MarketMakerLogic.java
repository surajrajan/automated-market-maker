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
     * @param swapContract
     * @param liquidityPool
     */
    public static LiquidityPool applySwapToPool(final SwapEstimate swapContract,
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

        AssetAmount inAssetAmount = swapContract.getInAssetAmount();
        AssetAmount outAssetAmount = swapContract.getOutAssetAmount();

        // depending on order, swap the supply
        if (swapContract.getInName().equals(assetNameOne)) {
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

    public static SwapEstimate createSwapEstimate(final LiquidityPool liquidityPool,
                                                  final SwapRequest request) {
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

        SwapEstimate swapEstimate = SwapEstimate.builder()
                .inName(request.getAssetNameIn())
                .inAssetAmount(AssetAmount.builder()
                        .amount(request.getAssetAmountIn())
                        .price(inPrice)
                        .build())
                .outName(request.getAssetNameOut())
                .outAssetAmount(AssetAmount.builder()
                        .amount(assetAmountOut)
                        .price(assetOutNewPrice)
                        .build())
                .build();
        return swapEstimate;
    }
}

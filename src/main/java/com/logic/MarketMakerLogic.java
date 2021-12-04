package com.logic;

import com.api.handler.swap.EstimateSwapHandler;
import com.model.AssetInfo;
import com.model.LiquidityPool;
import com.model.SwapContract;
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
     * @param constantMarketCap
     */
    public static void applySwapToPool(final SwapContract swapContract,
                                       final LiquidityPool liquidityPool,
                                       final Double constantMarketCap) {
        final String assetNames[] = liquidityPool.getLiquidityPoolName().split("-");
        final String assetNameOne = assetNames[0];
        if (swapContract.getAssetNameIn().equals(assetNameOne)) {
            liquidityPool.setAssetOneSupply(liquidityPool.getAssetOneSupply() + swapContract.getAssetAmountIn());
            liquidityPool.setAssetTwoSupply(liquidityPool.getAssetTwoSupply() - swapContract.getAssetAmountOut());
        } else {
            liquidityPool.setAssetOneSupply(liquidityPool.getAssetOneSupply() - swapContract.getAssetAmountOut());
            liquidityPool.setAssetTwoSupply(liquidityPool.getAssetTwoSupply() + swapContract.getAssetAmountIn());
        }
        liquidityPool.setAssetOneLocalPrice(constantMarketCap / liquidityPool.getAssetOneSupply());
        liquidityPool.setAssetTwoLocalPrice(constantMarketCap / liquidityPool.getAssetTwoSupply());
    }

    public static SwapContract createSwapContract(final LiquidityPool liquidityPool,
                                                  final EstimateSwapHandler.EstimateSwapRequest request) {
        // create map to access assetName to assetInfo
        Map<String, AssetInfo> assetAmountList = LiquidityPoolUtil.getAssetNameAssetAmountMap(liquidityPool);

        // calculate details about asset being swapped in / out
        AssetInfo assetInInfo = assetAmountList.get(request.getAssetNameIn());
        AssetInfo assetOutInfo = assetAmountList.get(request.getAssetNameOut());
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

        SwapContract swapContract = SwapContract.builder()
                .assetNameIn(request.getAssetNameIn())
                .assetAmountIn(request.getAssetAmountIn())
                .assetPriceIn(inPrice)
                .assetNameOut(request.getAssetNameOut())
                .assetAmountOut(assetAmountOut)
                .assetPriceOut(assetOutNewPrice)
                .build();
        return swapContract;
    }
}

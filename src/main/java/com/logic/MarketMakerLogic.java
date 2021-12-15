package com.logic;

import com.model.LiquidityPool;
import com.model.PriceAmount;
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
     * Given a SwapEstimate and LiquidityPool, constructs a new LiquidityPool with the SwapEstimate applied.
     *
     * @param swapEstimate
     * @param liquidityPool
     */
    public LiquidityPool applySwapEstimateToPool(@NonNull final SwapEstimate swapEstimate,
                                                 @NonNull final LiquidityPool liquidityPool) {
        // create a new pool to return
        final LiquidityPool newLiquidityPool = new LiquidityPool();
        newLiquidityPool.setPoolName(liquidityPool.getPoolName());
        PriceAmount newPriceAmountOne = new PriceAmount();
        PriceAmount newPriceAmountTwo = new PriceAmount();

        // create map to access assetName to assetInfo to get details about asset being swapped in / out
        Map<String, PriceAmount> nameToAssetAmountMap = LiquidityPoolUtil.getAssetNameToPriceAmountMap(liquidityPool);
        String assetNameOne = nameToAssetAmountMap.entrySet().iterator().next().getKey();

        // depending on order, make the swap in amount / supply
        if (swapEstimate.getInName().equals(assetNameOne)) {
            newPriceAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() + swapEstimate.getInPriceAmount().getAmount());
            newPriceAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() - swapEstimate.getOutPriceAmount().getAmount());
        } else {
            newPriceAmountOne.setAmount(liquidityPool.getAssetOne().getAmount() - swapEstimate.getInPriceAmount().getAmount());
            newPriceAmountTwo.setAmount(liquidityPool.getAssetTwo().getAmount() + swapEstimate.getInPriceAmount().getAmount());
        }

        // maintain market cap and calculate the new expected prices
        final Double constantMarketCapOne = liquidityPool.getAssetOne().getAmount() * liquidityPool.getAssetOne().getPrice();
        newPriceAmountOne.setPrice(constantMarketCapOne / newPriceAmountOne.getAmount());
        newPriceAmountTwo.setPrice(constantMarketCapOne / newPriceAmountTwo.getAmount());
        newLiquidityPool.setAssetOne(newPriceAmountOne);
        newLiquidityPool.setAssetTwo(newPriceAmountTwo);
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
        Map<String, PriceAmount> nameToAssetAmountMap = LiquidityPoolUtil.getAssetNameToPriceAmountMap(liquidityPool);
        PriceAmount assetInInfo = nameToAssetAmountMap.get(swapRequest.getInName());
        PriceAmount assetOutInfo = nameToAssetAmountMap.get(swapRequest.getOutName());

        // calculate constant k to maintain
        Double constantMarketCap = assetInInfo.getAmount() * assetInInfo.getPrice();
        Double k = constantMarketCap * constantMarketCap;

        // calculate market cap being added
        Double marketCapBeingSwappedIn = swapRequest.getInAmount() * assetInInfo.getPrice();
        Double newMarketCapIn = constantMarketCap + marketCapBeingSwappedIn;

        // use constant k to determine PriceAmount out
        Double newMarketCapOut = k / newMarketCapIn;
        Double marketCapChange = constantMarketCap - newMarketCapOut;
        Double assetAmountOut = marketCapChange / assetOutInfo.getPrice();
        Double assetOutNewPrice = constantMarketCap / newMarketCapOut * assetOutInfo.getPrice();

        SwapEstimate swapEstimate = SwapEstimate.builder()
                .inName(swapRequest.getInName())
                .inPriceAmount(PriceAmount.builder()
                        .amount(swapRequest.getInAmount())
                        .price(assetInInfo.getPrice())
                        .build())
                .outName(swapRequest.getOutName())
                .outPriceAmount(PriceAmount.builder()
                        .amount(assetAmountOut)
                        .price(assetOutNewPrice)
                        .build())
                .build();
        log.info("Created swap estimate: {}", swapEstimate);
        return swapEstimate;
    }
}

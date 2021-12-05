package com.util;

import com.config.ErrorMessages;
import com.model.AssetAmount;
import com.model.LiquidityPool;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.*;

@Slf4j
public final class LiquidityPoolUtil {

    public static Map<String, AssetAmount> getAssetNameAssetAmountMap(final LiquidityPool liquidityPool) {
        final String assetNames[] = liquidityPool.getLiquidityPoolName().split("-");
        final String assetNameOne = assetNames[0];
        final String assetNameTwo = assetNames[1];
        final AssetAmount assetAmountOne = AssetAmount.builder()
                .amount(liquidityPool.getAssetOne().getAmount())
                .price(liquidityPool.getAssetOne().getPrice())
                .build();
        final AssetAmount assetAmountTwo = AssetAmount.builder()
                .amount(liquidityPool.getAssetTwo().getAmount())
                .price(liquidityPool.getAssetTwo().getPrice())
                .build();

        // ordered map
        Map<String, AssetAmount> nameToAssetAmountMap = new TreeMap<>();
        nameToAssetAmountMap.put(assetNameOne, assetAmountOne);
        nameToAssetAmountMap.put(assetNameTwo, assetAmountTwo);

        return nameToAssetAmountMap;
    }

    public static String getLiquidityPoolName(final String assetNameOne, final String assetNameTwo) {
        int compare = assetNameOne.compareTo(assetNameTwo);
        if (compare < 0) {
            return MessageFormat.format("{0}-{1}", assetNameOne, assetNameTwo);
        } else if (compare > 0) {
            return MessageFormat.format("{0}-{1}", assetNameTwo, assetNameOne);
        } else {
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ASSET);
        }
    }

    public static void logKValue(final LiquidityPool liquidityPool) {
        // calculate stats for asset one and asset two
        final AssetAmount assetAmountOne = liquidityPool.getAssetOne();
        final AssetAmount assetAmountTwo = liquidityPool.getAssetTwo();
        final Double constantMarketCapOne = assetAmountOne.getAmount() * assetAmountOne.getPrice();
        final Double constantMarketCapTwo = assetAmountTwo.getAmount() * assetAmountTwo.getPrice();
        final Double k = constantMarketCapOne * constantMarketCapOne;
        log.info("constantMarketCapOne: {}, constantMarketCapOne: {}, k: {}",
                constantMarketCapOne, constantMarketCapTwo, k);
    }
}

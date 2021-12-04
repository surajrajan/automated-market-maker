package com.util;

import com.config.ErrorMessages;
import com.model.AssetInfo;
import com.model.LiquidityPool;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.*;

@Slf4j
public final class LiquidityPoolUtil {

    public static Map<String, AssetInfo> getAssetNameAssetAmountMap(final LiquidityPool liquidityPool) {
        final String assetNames[] = liquidityPool.getLiquidityPoolName().split("-");
        final String assetNameOne = assetNames[0];
        final String assetNameTwo = assetNames[1];
        List<AssetInfo> assetInfoList = new ArrayList<>();
        final AssetInfo assetAmountOne = AssetInfo.builder()
                .assetName(assetNameOne)
                .amount(liquidityPool.getAssetOneSupply())
                .price(liquidityPool.getAssetOneLocalPrice())
                .build();
        final AssetInfo assetAmountTwo = AssetInfo.builder()
                .assetName(assetNameTwo)
                .amount(liquidityPool.getAssetTwoSupply())
                .price(liquidityPool.getAssetTwoLocalPrice())
                .build();
        assetInfoList.add(assetAmountOne);
        assetInfoList.add(assetAmountTwo);
        Collections.sort(assetInfoList, Comparator.comparing(AssetInfo::getAssetName));
        Map<String, AssetInfo> nameToAssetAmountMap = new LinkedHashMap<>();
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
        // calculate amount returned
        final Double constantMarketCap = liquidityPool.getAssetOneSupply() * liquidityPool.getAssetOneLocalPrice();
        final Double k = constantMarketCap * constantMarketCap;
        log.info("constantMarketCap: {}, k: {}", constantMarketCap, k);
    }
}

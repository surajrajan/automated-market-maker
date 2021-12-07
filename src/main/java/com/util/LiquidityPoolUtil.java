package com.util;

import com.config.ErrorMessages;
import com.model.AssetAmount;
import com.model.EstimateSwapRequest;
import com.model.LiquidityPool;
import com.model.exception.InvalidInputException;
import com.model.types.Asset;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

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


    public static String extractLiquidityPoolNameFromPathParams(final Map<String, String> pathParameters)
            throws InvalidInputException {
        if (pathParameters == null) {
            throw new InvalidInputException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        String liquidityPoolName = pathParameters.get("liquidityPoolName");
        if (liquidityPoolName == null || liquidityPoolName.isEmpty()) {
            throw new InvalidInputException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        validateLiquidityPoolName(liquidityPoolName);
        return liquidityPoolName;
    }

    public static String inferLiquidityPoolFromSwapRequest(final EstimateSwapRequest estimateSwapRequest) {
        String assetIn = estimateSwapRequest.getAssetNameIn();
        String assetOut = estimateSwapRequest.getAssetNameOut();
        int compare = assetIn.compareTo(assetOut);
        if (compare < 0) {
            return MessageFormat.format("{0}-{1}", assetIn, assetOut);
        }
        return MessageFormat.format("{0}-{1}", assetOut, assetIn);
    }

    private static void validateLiquidityPoolName(final String liquidityPoolName) throws InvalidInputException {
        String assetNames[] = liquidityPoolName.split("-");
        if (assetNames.length != 2) {
            throw new InvalidInputException(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME);
        }
        String assetOne = assetNames[0];
        String assetTwo = assetNames[1];
        if (!Asset.isValidAssetName(assetOne) || !Asset.isValidAssetName(assetTwo)) {
            throw new InvalidInputException(ErrorMessages.INVALID_ASSET_NAME);
        }
        if (assetOne.equals(assetTwo)) {
            throw new InvalidInputException(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME);
        }
        if (assetOne.compareTo(assetTwo) >= 0) {
            throw new InvalidInputException(ErrorMessages.INVALID_LIQUIDITY_POOL_NAME_ASSET_ORDER);
        }
    }
}

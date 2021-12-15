package com.util;

import com.config.ErrorMessages;
import com.model.LiquidityPool;
import com.model.PriceAmount;
import com.model.SwapRequest;
import com.model.exception.InvalidInputException;
import com.model.types.Asset;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for various operations on LiquidityPool and names.
 */
@Slf4j
public final class LiquidityPoolUtil {

    /**
     * Builds a map of the asset name to PriceAmount based on the LiquidityPool object.
     *
     * @param liquidityPool
     * @return Map<String, PriceAmount>
     */
    public static Map<String, PriceAmount> getAssetNameToPriceAmountMap(@NonNull final LiquidityPool liquidityPool) {
        final String assetNames[] = liquidityPool.getPoolName().split("-");
        final String assetNameOne = assetNames[0];
        final String assetNameTwo = assetNames[1];
        final PriceAmount priceAmountOne = PriceAmount.builder()
                .amount(liquidityPool.getAssetOne().getAmount())
                .price(liquidityPool.getAssetOne().getPrice())
                .build();
        final PriceAmount priceAmountTwo = PriceAmount.builder()
                .amount(liquidityPool.getAssetTwo().getAmount())
                .price(liquidityPool.getAssetTwo().getPrice())
                .build();
        // ordered map
        Map<String, PriceAmount> nameToAssetAmountMap = new TreeMap<>();
        nameToAssetAmountMap.put(assetNameOne, priceAmountOne);
        nameToAssetAmountMap.put(assetNameTwo, priceAmountTwo);
        return nameToAssetAmountMap;
    }

    /**
     * Builds the liquidity pool name from two asset names, ordering them alphabetically.
     *
     * @param assetNameOne
     * @param assetNameTwo
     */
    public static String getPoolName(@NonNull final String assetNameOne, @NonNull final String assetNameTwo) {
        int compare = assetNameOne.compareTo(assetNameTwo);
        if (compare < 0) {
            return MessageFormat.format("{0}-{1}", assetNameOne, assetNameTwo);
        } else if (compare > 0) {
            return MessageFormat.format("{0}-{1}", assetNameTwo, assetNameOne);
        } else {
            throw new IllegalArgumentException(ErrorMessages.DUPLICATE_ASSET);
        }
    }

    /**
     * Logs stats about the liquidity pool.
     *
     * @param liquidityPool
     */
    public static void logStats(@NonNull final LiquidityPool liquidityPool) {
        // calculate stats for asset one and asset two
        final PriceAmount priceAmountOne = liquidityPool.getAssetOne();
        final PriceAmount priceAmountTwo = liquidityPool.getAssetTwo();
        final Double constantMarketCapOne = priceAmountOne.getAmount() * priceAmountOne.getPrice();
        final Double constantMarketCapTwo = priceAmountTwo.getAmount() * priceAmountTwo.getPrice();
        final Double k = constantMarketCapOne * constantMarketCapOne;
        log.info("constantMarketCapOne: {}, constantMarketCapOne: {}, k: {}",
                constantMarketCapOne, constantMarketCapTwo, k);
    }

    /**
     * Infers the liquidity pool name from in / out asset names in the swap request, ordering them alphabetically.
     *
     * @param swapRequest
     */
    public static String inferPoolNameFromSwapRequest(@NonNull final SwapRequest swapRequest) {
        String assetIn = swapRequest.getInName();
        String assetOut = swapRequest.getOutName();
        int compare = assetIn.compareTo(assetOut);
        if (compare < 0) {
            return MessageFormat.format("{0}-{1}", assetIn, assetOut);
        }
        return MessageFormat.format("{0}-{1}", assetOut, assetIn);
    }

    /**
     * Validates that a liquidity pool name has correct asset names.
     *
     * @param liquidityPoolName
     * @throws InvalidInputException
     */
    public static void validateLiquidityPoolName(@NonNull final String liquidityPoolName) throws InvalidInputException {
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

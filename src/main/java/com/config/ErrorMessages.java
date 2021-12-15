package com.config;


import com.model.types.Asset;

import java.text.MessageFormat;

public class ErrorMessages {
    public static String INVALID_PRICE_RANGE = MessageFormat.format("Price must be set between {0} and {1}",
            ServiceConstants.MIN_PRICE, ServiceConstants.MAX_PRICE);
    public static String INVALID_SUPPLY_RANGE = MessageFormat.format("Supply must be set between {0} and {1}",
            ServiceConstants.MIN_SUPPLY, ServiceConstants.MAX_SUPPLY);
    public static String INVALID_ASSET_NAME = MessageFormat.format("Invalid asset. Supported assets: {0}",
            Asset.getValidAssetNames().toString());
    public static String DUPLICATE_ASSET = "Two different assets must be provided.";
    public static String INVALID_REQUEST_MISSING_FIELDS = "One or more fields missing for this request.";
    public static String LIQUIDITY_POOL_ALREADY_EXISTS = "Liquidity pool for this pair already exists.";
    public static String INVALID_LIQUIDITY_POOL_NAME = "Invalid name for liquidity pool.";
    public static String INVALID_LIQUIDITY_POOL_NAME_ASSET_ORDER = "Pool name must be created with assets in alphabetical order.";
    public static String LIQUIDITY_POOL_DOES_NOT_EXIST = "Liquidity pool does not exist.";
    public static String UNEQUAL_MARKET_CAP_LIQUIDITY_UPDATE = "Market caps of coins are unequal in this update.";
    public static String NEGATIVE_AMOUNT_TO_SWAP = "Positive number must be provided for amount to swap.";
    public static String INVALID_CLAIM = "Invalid swap claim.";
    public static String CLAIM_EXPIRED = "Claim is expired.";
    public static String CLAIM_ALREADY_USED = "Claim is already used.";
}

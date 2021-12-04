package com.config;


import java.text.MessageFormat;

public class ErrorMessages {
    public static String INVALID_PRICE_RANGE = MessageFormat.format("Price must be set between {0} and {1}",
            ServiceLimits.MIN_PRICE, ServiceLimits.MAX_PRICE);
    public static String INVALID_SUPPLY_RANGE = MessageFormat.format("Supply must be set between {0} and {1}",
            ServiceLimits.MIN_SUPPLY, ServiceLimits.MAX_SUPPLY);
    public static String INVALID_ASSET_NAME = MessageFormat.format("Invalid asset. Supported assets: {0}",
            ServiceConstants.ALLOWED_ASSETS.toString());
    public static String DUPLICATE_ASSET = "Two different assets must be provided.";
    public static String INVALID_REQUEST_MISSING_FIELDS = "One or more fields missing for this request.";
    public static String LIQUIDITY_POOL_ALREADY_EXISTS = "Liquidity pool for this pair already exists.";
    public static String INVALID_LIQUIDITY_POOL_NAME = "Liquidity pool does not exist.";
    public static String UNEQUAL_MARKET_CAP_LIQUIDITY_UPDATE = "Market caps of coins are unequal in this update.";
    public static String NOT_ENOUGH_LIQUIDITY = "Not enough liquidity / supply to make this swap.";
    public static String NEGATIVE_AMOUNT_TO_SWAP = "Positive number must be provided for amount to swap.";

}

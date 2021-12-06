package com.model.types;

import java.util.HashMap;
import java.util.Map;

public enum Asset {
    APPLES("Apples"),
    BANANAS("Bananas"),
    TOMATOES("Tomatoes"),
    POTATOES("Potatoes");

    String name;
    private static Map<String, Asset> nameToAssetMap = new HashMap<>();

    Asset(final String name) {
        this.name = name;
    }

    public static boolean isValidAssetName(final String assetName) {
        return nameToAssetMap.containsKey(assetName);
    }

    static {
        for (Asset asset : Asset.values()) {
            nameToAssetMap.put(asset.name, asset);
        }
    }
}
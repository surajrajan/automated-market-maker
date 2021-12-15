package com.model.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Possible values for asset.
 */
public enum Asset {
    APPLES("Apples"),
    BANANAS("Bananas"),
    ORANGES("Oranges"),
    PEARS("Pears"),
    LEMONS("Lemons"),
    LIMES("Limes"),
    TOMATOES("Tomatoes"),
    ONIONS("Onions"),
    CARROTS("Carrots"),
    POTATOES("Potatoes");

    String name;
    private static Map<String, Asset> nameToAssetMap = new HashMap<>();

    Asset(final String name) {
        this.name = name;
    }

    public static boolean isValidAssetName(final String assetName) {
        return nameToAssetMap.containsKey(assetName);
    }

    public static List<String> getValidAssetNames() {
        return nameToAssetMap.values().stream().map(a -> a.name).collect(Collectors.toList());
    }

    static {
        for (Asset asset : Asset.values()) {
            nameToAssetMap.put(asset.name, asset);
        }
    }
}
package com.config;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceConstants {
    public static Set<String> ALLOWED_ASSETS = Stream.of(
            "Apples",
            "Bananas",
            "Tomatoes",
            "Potatoes"
    ).collect(Collectors.toUnmodifiableSet());
}

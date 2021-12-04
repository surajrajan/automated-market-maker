package com.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssetInfo {
    private String assetName;
    private Double amount;
    private Double price;
}

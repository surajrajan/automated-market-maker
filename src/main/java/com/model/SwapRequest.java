package com.model;

import lombok.Data;

@Data
public class SwapRequest {
    private String assetNameIn;
    private Double assetAmountIn;
    private String assetNameOut;
}

package com.model.swap;

import lombok.Data;

@Data
public class EstimateSwapRequest {
    private String assetNameIn;
    private Double assetAmountIn;
    private String assetNameOut;
}

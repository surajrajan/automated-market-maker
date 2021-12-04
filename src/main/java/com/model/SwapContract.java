package com.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SwapContract {
    private String assetNameIn;
    private Double assetPriceIn;
    private Double assetAmountIn;
    private String assetNameOut;
    private Double assetPriceOut;
    private Double assetAmountOut;
}

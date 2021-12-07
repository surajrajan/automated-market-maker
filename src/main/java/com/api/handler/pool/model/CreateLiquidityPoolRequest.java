package com.api.handler.pool.model;

import com.model.AssetAmount;
import lombok.Data;

@Data
public class CreateLiquidityPoolRequest {
    private AssetAmount assetAmountOne;
    private AssetAmount assetAmountTwo;
}

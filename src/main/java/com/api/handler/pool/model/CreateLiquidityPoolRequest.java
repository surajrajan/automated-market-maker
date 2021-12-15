package com.api.handler.pool.model;

import com.model.PriceAmount;
import lombok.Data;

@Data
public class CreateLiquidityPoolRequest {
    private PriceAmount assetOne;
    private PriceAmount assetTwo;
}

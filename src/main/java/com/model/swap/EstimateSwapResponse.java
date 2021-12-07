package com.model.swap;

import com.model.SwapContract;
import lombok.Data;

@Data
public class EstimateSwapResponse {
    private String swapClaim;
    private SwapContract swapContract;
}

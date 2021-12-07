package com.api.handler.swap.model;

import com.model.SwapEstimate;
import lombok.Data;

@Data
public class EstimateSwapResponse {
    private String swapClaimToken;
    private SwapEstimate swapEstimate;
}

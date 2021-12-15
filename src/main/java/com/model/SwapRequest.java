package com.model;

import lombok.Data;

/**
 * A request that a user can make to swap. Requires in and out asset names. Only requires in the in asset amount,
 * as the automated market maker will return the amount the user can expect to receive back in the SwapEstimate.
 */
@Data
public class SwapRequest {
    private String inName;
    private Double inAmount;
    private String outName;
}

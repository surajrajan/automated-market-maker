package com.model;

import lombok.Data;

@Data
public class SwapRequest {
    private SwapContract swapContract;
    private String transactionId;
}

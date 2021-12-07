package com.model;

import lombok.Data;

import java.util.Date;

@Data
public class SwapClaim {
    private String swapContractId;
    private Date expiresAt;
    private SwapRequest swapRequest;
}

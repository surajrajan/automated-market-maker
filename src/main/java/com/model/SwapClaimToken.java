package com.model;

import lombok.Data;

import java.util.Date;

@Data
public class SwapClaimToken {
    private String swapContractId;
    private Date expiresAt;
    private SwapRequest swapRequest;
}

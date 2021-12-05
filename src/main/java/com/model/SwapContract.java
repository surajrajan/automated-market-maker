package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@DynamoDBDocument
public class SwapContract {
    private String assetNameIn;
    private Double assetPriceIn;
    private Double assetAmountIn;
    private String assetNameOut;
    private Double assetPriceOut;
    private Double assetAmountOut;
    private Date expiresAt;
}

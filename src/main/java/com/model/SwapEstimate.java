package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An estimate for a swap being performed in a liquidity pool. Contains the exact in / out amounts that a one can expect
 * that the automated market maker calculated.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@DynamoDBDocument
public class SwapEstimate {
    private String inName;
    private PriceAmount inPriceAmount;
    private String outName;
    private PriceAmount outPriceAmount;
}

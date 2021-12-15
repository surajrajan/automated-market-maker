package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Conveys the amount and price of an Asset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBDocument
public class PriceAmount {
    @NonNull
    private Double price;
    @NonNull
    private Double amount;
}

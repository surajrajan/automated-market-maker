package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBDocument
public class AssetAmount {
    @NonNull
    private Double amount;
    @NonNull private Double price;
}

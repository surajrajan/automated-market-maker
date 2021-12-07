package com.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@DynamoDBDocument
public class SwapEstimate {
    private String inName;
    private AssetAmount inAssetAmount;
    private String outName;
    private AssetAmount outAssetAmount;
}

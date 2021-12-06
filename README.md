# Automated Market Maker

An automated market maker is a type of market.

This project simulates an automated market maker by allowing users to create liquidity pools between arbitrary assets,
place trades / swaps between assets and observe price action as trades are made.

## Setup
* Set up serverless CLI / AWS CLI
```
sls deploy
```
* For now, need to create a KMS Symmetric Key. 
* Link the KMS Key ARN in `com.client.kms.KMSModule`

## Example

### 1) Create Liquidity Pool
Start by creating a liquidity pool (trading pool) between any two pairs of assets.

**API Name:** `CreateLiquidityPool`
```
sls invoke --function createLiquidityPool --path apiTestData/createLiquidityPool.json
```

**Example Request:**
```
{
  "assetOne": {
    "name": "Apples",
    "initialPrice": 100,
    "initialSupply": 50000
  },
  "assetTwo": {
    "name": "Bananas",
    "initialPrice": 100,
    "initialSupply": 50000
  }
}
```
**Requirements**
* Market cap of both assets **must** be equal
* All supported assets are documented in `com.model.types.Asset` enum.

**Response**

*Example Response*

**GetLiquidityPool:**
```
sls invoke --function getLiquidityPool --path apiTestData/getLiquidityPool.json
```

**EstimateSwap:**
```
 sls invoke --function estimateSwap --path apiTestData/estimateSwap.json
```

**SubmitSwap:**
```
 sls invoke --function submitSwap --path apiTestData/submitSwap.json
```
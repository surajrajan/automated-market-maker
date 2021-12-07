# Automated Market Maker

![Alt text](images/readme.jpg?raw=true "Title")

An Automated Market Maker (AMM) is an entity in decentralized finance (DeFi) where users can swap two assets
**completely autonomously without a third party**. The AMM automatically sets / maintains of the prices of the assets
as swaps / trades are made. This is contrary to traditional centralized market systems where limit buy / sell orders
are matched to set the prices.

To best understand how an automated market maker works, see this [youtube video](https://www.youtube.com/watch?v=1PbZMudPP5E) (also
the credit for the above thumbnail). 

## Project Overview
This project is an AWS [serverless](https://www.serverless.com/framework/docs) project, which, when deployed, provides
a set APIs (each backed by a Lambda function), interacting with DynamoDB, SQS, and KMS that can help simulate an AMM:

* **Create Liquidity Pool:** Creates ```LiquidityPool``` between any two assets. Allowed asset names are in `com.model.types.Asset`.
    Initial prices / supply of both assets must be set such that the **market cap** of each is equal. Stored in the
    DynamoDB LiquidityPools table. 
* **Get Liquidity Pool:** Returns the details of a liquidity pool, along with price / supply details both assets.
* **Estimate Swap:** Estimates the details of performing an ```SwapRequest```. Requires a liquidity pool to exist for 
    the assets being swapped. Response is a ```SwapEstimate```, which details the amount / price of the asset received
    back. Also returns a ```swapClaimToken```, which is a **one-time use token** to redeem / submit this transaction. It
    is not guaranteed that the actual price will be exactly the same as the estimate, but it will be processed.
* **Submit Swap:** Given a ```swapClaimToken```, will queue the underlying ```SwapRequest``` request into an SQS queue,
    which will then be processed in real time. As the swap claim is processed, a new ```SwapEstimate``` is constructed.
    This swap is then **executed** by updating the LiquidityPool. The ```Transaction``` is also recorded in the DynamoDB
    Transactions table.

**Note:** In a real AMM, users would have the ability to set a *slippage* parameter that prevents actual swap to be too
far off from the initial estimate.

### How does the AMM maintain prices?
One standard way an AMM calculates price action is using the **constant product formula** - Whenever a
trade (swap of assets) is made in the pool, the market maker will **maintain a constant k-value**, which is calculated by:
```
k = marketCapAssetOne * marketCapAssetTwo
```
While maintaining this value, the AMM can set before / after prices. The logic for this algorithm is contained in `com.logic.MarketMakerLogic`.

## Setup
**Pre-Requisites**
* Set up serverless CLI / AWS CLI
* Install java 11
* Create **KMS Symmetric Key** in AWS console
* Set the Key ARN in `com.client.kms.KMSConstants` before deploying code

**Set Up / Deploy**
* Clone project: ```git clone https://github.com/surajrajan/automated-market-maker```
* Build project locally: ```./gradlew build```
* Deploy project to AWS: ```sls deploy```
* **Note:** currently supports only ```dev``` stage - configure as necessary in ```serverless.yml```

## Examples
It's easiest to understand the project while interfacing with each of the APIs individually.

### CreateLiquidityPool
Start by creating a liquidity pool (trading pool) between any two pairs of assets. Initial market cap of each asset
(price * amount) **must be equal**.

```
sls invoke --function createLiquidityPool --path apiTestData/createLiquidityPool.json
```

**Example Response**
```
{
    "statusCode": 204,
    "headers": {
        "RequestId": "449570e1-a579-4f8a-b95b-d895b0cf2882"
    }
}
```

### GetLiquidityPool
Gets the details of a liquidity pool.
```
sls invoke --function getLiquidityPool --path apiTestData/getLiquidityPool.json
```

**Example Response:**
```
{
    "statusCode": 200,
    "body": "{\"liquidityPoolName\":\"Apples-Bananas\",\"assetOne\":{\"amount\":50000.0,\"price\":100.0},\"assetTwo\":{\"amount\":50000.0,\"price\":100.0},\"version\":1}",
    "headers": {
        "RequestId": "5399f461-fed8-4c23-b365-a0d8f7433db1"
    }
}
```

### EstimateSwap
Provides an estimate / swap contract for the swap if valid. Contains a claim token that **must be passed** back to the
SubmitSwap API to execute it.

```
 sls invoke --function estimateSwap --path apiTestData/estimateSwap.json
```

**Example Response:**
```
{
    "statusCode": 200,
    "body": "{\"swapClaimToken\":\"someEncryptedClaim\",\"swapContract\":{\"inName\":\"Apples\",\"inAssetAmount\":{\"amount\":7000.0,\"price\":100.0},\"outName\":\"Bananas\",\"outAssetAmount\":{\"amount\":6140.35087719298,\"price\":113.99999999999999},\"expiresAt\":1638784090002}}",
    "headers": {
        "RequestId": "d5eee705-b27a-4d4a-bcf8-d7a74e594af0"
    }
}
```

### SubmitSwap
Submits a SwapContract for execution. The swap contract is encoded by the encrypted swap claim that is provided by the
EstimateSwap API.

```
 sls invoke --function submitSwap --path apiTestData/submitSwap.json
```

**Example Response:**
```
{
    "statusCode": 200,
    "body": "{\"transactionId\":\"7cee48c2-81bd-48ce-b63a-e3ca47c558a9\"}",
    "headers": {
        "RequestId": "4587e59c-0924-4fd0-9c35-dc164165f346"
    }
}
```

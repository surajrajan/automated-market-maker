# Automated Market Maker

An automated market maker (AMM) is an entity that, without a third party, can automatically allow the swapping of
any two assets and set / maintain prices of the assets. This is contrary to traditional centralized market systems that
set prices by matching limit buy / sell orders. An AMM works by having a "constant liquidity", or a "supply" of both assets
to be swapped in a dedicated pool.

One standard way an AMM calculates price action is using the **constant product formula** - At a high level, whenever a
trade (swap of assets) is made in the pool, the market maker will **maintain** a constant **K-value**, which is calculated by:

```
k = marketCapAssetOne * marketCapAssetTwo
```

Based on this, the supply and prices of the assets are calculated after a swap.
For a more detailed overview, see https://www.youtube.com/watch?v=1PbZMudPP5E


There are a few APIs in the project to help simulate an AMM:
* **Create Liquidity Pool**
  * Allows the creation of a liquidity pool between any two assets with configurable starting prices / supply
  * When a pool is created, it **must** have equal starting market cap value
* **Get Liquidity Pool**
    * Returns the details of a liquidity pool
* **Estimate Swap**
  * Given a liquidity pool, a **swap** must be requested, which will provide a quote / contract for the swap
  * One of the response parameters is a **claim**, which can then be used to **submit** / finalize the transaction
* **Submit Swap**
  * Submits the swap to be executed by placing it into a queue
  * Swap transactions are handled by the queue, the liquidity pool details are updated, and the transaction is recorded with all the details

## Setup
### Pre-req
* Set up serverless CLI / AWS CLI
* Install java 11

### Build
```
./gradlew build
```
### Deploy
```
sls deploy
```
**Note:**
* Create **KMS Symmetric Key** in AWS console
* Set the Key ARN in `com.client.kms.KMSConstants` before deploying code

## Example

### Create Liquidity Pool
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

**Example Response**
```
{
    "statusCode": 204,
    "headers": {
        "RequestId": "449570e1-a579-4f8a-b95b-d895b0cf2882"
    }
}
```

### Get Liquidity Pool
Gets the details of a liquidity pool.

**API Name:** `GetLiquidityPool`
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

### Estimate Swap
Provides an estimate / swap contract for the swap if valid. Contains a claim token that needs to be be passed
back on submit swap API to submit the swap.

**API Name:** `Estimate Swap`
```
 sls invoke --function estimateSwap --path apiTestData/estimateSwap.json
```

**Example Request:**
```
{
  "assetNameIn": "Apples",
  "assetAmountIn": 7000,
  "assetNameOut": "Bananas"
}
```

**Example Response:**
```
{
    "statusCode": 200,
    "body": "{\"swapClaim\":\"AgV40z1TxzMfWcRtusgWsOk1WKAthF224eHNlgaZbZ2uQp8AXwABABVhd3MtY3J5cHRvLXB1YmxpYy1rZXkAREExOXJ6OUt5OS9GdVdHajl5SUo3UXVLZlB0Um44UkZneGRnVUVMbXhFa2tOMHlkSG9vTkZmTjFQQVVGeEpYZmVXQT09AAEAB2F3cy1rbXMAS2Fybjphd3M6a21zOnVzLWVhc3QtMTo4NjEwNjA3MTQxMjU6a2V5L2JiZTJlNjNiLWRmYjQtNGI1NS1iNzdiLTEyZTc2NjZmN2I0YQC4AQIBAHhCZM0mN8+FmBmqT4/JZfwJsUzvBO6TKj/WSwbN4RsSUgHsLXrmiHe60m4/lS1uV9AAAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMoI83hkW7IIt94VpVAgEQgDviiTpI4axA6qBtkaxQfFPXFe64aJVSIqjuQRufBtqZDxZ2WNVa+aqzpacK+H4FDL+hc9wwkiCVz82E1QIAABAAGeGPSuzprgzqCqW6DYzxd2MKEYfumiBhZC5bLYeZfA9SbjXUAplrWSrae7HmSaZI/////wAAAAEAAAAAAAAAAAAAAAEAAAC5Gy3cY/GVXgI/RvdG+Y19XdxzLIV5tv7u05LJ9EeSqetb73CKWcly9b3Mem4l9wI6P8hDRQd0cS56Rcp17SOaLUP4bu55A6F6HBC1u37CMx5MB4uxCiOQw58mFEdFCnRtXAz71ViNmR2y8IBzxEkdCbof0gSHJfl+uVrrHkHLyJjtyCl2VRD3VMV47DefiC77OTct2h0Fkw9DRjeEVjzyUZXtu2AGligyzC4mzMZPHuTvQgl63NeL0dnkSBq3kvbNkgJOZmuQP8NlAGcwZQIwF7ZItRJ3LG+1v1YA/6aGYSPLOaNGOa6qZWPUvmYDzCtOzEYYlhCR9v587ScIgAxYAjEAqYu3ddgbzcfi5aWm+g9Re0Ett+C1xzUfIVzPX24yAD4AbHK3uKcHxy+uUMY15Ms3\",\"swapContract\":{\"inName\":\"Apples\",\"inAssetAmount\":{\"amount\":7000.0,\"price\":100.0},\"outName\":\"Bananas\",\"outAssetAmount\":{\"amount\":6140.35087719298,\"price\":113.99999999999999},\"expiresAt\":1638784090002}}",
    "headers": {
        "RequestId": "d5eee705-b27a-4d4a-bcf8-d7a74e594af0"
    }
}
```

### Submit Swap
**API Name:** `Estimate Swap`
```
 sls invoke --function submitSwap --path apiTestData/submitSwap.json
```

**Example Request:**
```
{
  "swapClaim": "AgV40z1TxzMfWcRtusgWsOk1WKAthF224eHNlgaZbZ2uQp8AXwABABVhd3MtY3J5cHRvLXB1YmxpYy1rZXkAREExOXJ6OUt5OS9GdVdHajl5SUo3UXVLZlB0Um44UkZneGRnVUVMbXhFa2tOMHlkSG9vTkZmTjFQQVVGeEpYZmVXQT09AAEAB2F3cy1rbXMAS2Fybjphd3M6a21zOnVzLWVhc3QtMTo4NjEwNjA3MTQxMjU6a2V5L2JiZTJlNjNiLWRmYjQtNGI1NS1iNzdiLTEyZTc2NjZmN2I0YQC4AQIBAHhCZM0mN8"
}
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

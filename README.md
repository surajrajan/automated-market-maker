# Automated Market Maker

![Alt text](images/readme.jpg?raw=true "Automated Market Maker")

# Background
An Automated Market Maker (AMM) is an entity in decentralized finance (DeFi) where users can swap two assets
**completely autonomously without a third party**. The AMM automatically sets / maintains of the prices of the assets
as swaps / trades are made. This is contrary to traditional centralized market systems where limit buy / sell orders
are matched to set the prices.

To best understand how an automated market maker works, see this [youtube video](https://www.youtube.com/watch?v=1PbZMudPP5E) (also
the credit for the above thumbnail). 

# Project Overview
This project is an AWS [serverless](https://www.serverless.com/framework/docs) project, which, when deployed, provides
a set APIs (each backed by a Lambda function), that interact with DynamoDB, SQS, and KMS and simulate an AMM. 

* Model Definitions are in ```com.model```
* Client facing APIs are documented in ```com.api.handle```
* API request examples discussed below are in ```apiTestData``` folder to use

## Client Facing APIs

### CreateLiquidityPool ```POST /pool/{poolName}```
Creates a ```LiquidityPool``` for two ```PriceAmount``` objects, signifying the starting prices / supplies of the two
assets. When created, the initial prices / supply of both assets must be set such that the **market cap** of
each is equal. Stored in DynamoDB. The asset names in the ```poolName``` from the path must be in alphabetical order.


**Request Definition:**

| parameterType | parameterName |
| ----------- | ----------- |
| ```PriceAmount```      | assetOne       |
| ```PriceAmount```   | assetTwo        |


**Example Request / Response:**
```
Request

POST /pool/Apples-Bananas

{
  "assetOne": {
    "price": 100,
    "amount": 50000
  },
  "assetTwo": {
    "price": 100,
    "amount": 50000
  }
}

Response

201 Created
```
**Response**
* 201 Created

### GetLiquidityPool ```GET /pool/{poolName}```
Returns the ```LiquidityPool``` object by name from DynamoDB.

**Example Request / Response**
```
Request

GET /pool/Apples-Bananas

Response

{
  "name": "Apples-Bananas",
  "assetOne": {
    "amount": 50000,
    "price": 100
  },
  "assetTwo": {
    "amount": 50000,
    "price": 100
  },
  "createdTime: 1639600395339
  "updatedTime: 1639600395339
}
```

### EstimateSwap ```POST /swap/estimate```
Estimates the details of performing a ```SwapRequest```. Requires a corresponding ```LiquidityPool```
to exist to provide liquidity for the assets being swapped. Response includes 1) a ```SwapEstimate```, detailing the
current exact ```AssetAmount``` details being swapped in / out, and 2) a ```swapClaimToken```, a **one-time use token**
that can be used to redeem / submit the requested swap. Note, the amount here is just an estimate, and may be different
by the time the swap is submitted / processed.

**Request Definition** 

| parameterType | parameterName |
| ----------- | ----------- |
| ```SwapRequest```      | swapRequest |

**Response Definition**

| parameterType | parameterName |
| ----------- | ----------- |
| ```SwapEstimate```      | swapEstimate       |
| ```String```   | swapClaimToken        |

**Example Request**
```
Request 

POST /swap/estimate

{
    "swapRequest" : {
        "inName": "Apples",
        "inAmount" "7000",
        "outName" Bananas
    }
}

Response

{
   "swapClaimToken": "AgV4mhdSSNpB3KxejkfkSxU036JdfpMgzKiIcdOd4XAizBYAXwABABVhd3MtY3J5cHRvLXB1YmxpYy1rZXkAREF2WXNTLzhNejl6dVEwNnk1ZzQ1Wm1zblFTaHNyY0xXbzZQQW10YW0xQ1dnRTB6UTZyREZTeGVDU1Bjd0JGSk9mZz09AAEAB2F3cy1rbXMAS2Fybjphd3M6a21zOnVzLWVhc3QtMTo4NjEwNjA3MTQxMjU6a2V5L2JiZTJlNjNiLWRmYjQtNGI1NS1iNzdiLTEyZTc2NjZmN2I0YQC4AQIBAHhCZM0mN8+FmBmqT4/JZfwJsUzvBO6TKj/WSwbN4RsSUgEZyUN/tfqKuzf4MJOtvzrvAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMKoeof5JPXigu26AKAgEQgDuUrjP9ZCF+ijh5UWxUM/gR1f9WGpw4xZz7Xttx+jK3wT02/Eclai6pTc9zuqts+KixXB1NZvrgGxYWRAIAABAAvVh5CDrQ+pnqOUppN6WwR2PBmTLBl2x0ffS0HVnjGzHLI5C+DCU2F+fUnzaGCOy0/////wAAAAEAAAAAAAAAAAAAAAEAAACbiPfGEXGnzOTS5hTLumWa2x/oF3z7jk6dw8UDWAPvI1D7wcXFO++iIrpc9F6FVDVVOIy1mOUkKjUj6782Sb1Jta1avkTjKmnJq6fg1IRTjAK4PY+7UkImSxIQ5xZVhqEqJALhFOXp4mmckyS/lmAxNj7yh4PH7tDCYj1dc68cRFz6oUqDQTtLxSlYfWyA69rXLCBrusONsUXDsSkremGydx0Kl6eSaSfs01JYAGcwZQIwbkE4Fi/X8RjqHLPvSsQJkvWVP9zBX74IA7WtUmag/tLvj5UoX5neaEhT13D3NWP7AjEAzmHrVWkNm2dPBlrCvMYJ+sa4gqkj252NsJ3tdNcDxE8TbNhfHkcTQW6hskRM4VXh",
   "swapEstimate": {
      "inName": "Apples",
      "inPriceAmount": {
         "price": 100,
         "amount": 7000
      },
      "outName": "Bananas",
      "outPriceAmount": {
         "price": 114,
         "amount": 6140.351
      }
   }
}
```

### SubmitSwap ```POST /swap/submit```
Given a ```swapClaimToken```, will queue the underlying ```SwapRequest``` request into an SQS queue,
which will then be processed in real time. As the swap claim is processed, a new ```SwapEstimate``` is constructed. Note,
this means that the swap actually performed may be slightly different than what was estimated. When the swap is
**executed**, the ```Transaction``` and new state of the ```LiquidityPool``` are updated in DynamoDB.

**Request Definition**

| parameterType | parameterName |
| ----------- | ----------- |
| ```swapClaimToken```      | swapClaimToken |

**Response Definition**

| parameterType | parameterName |
| ----------- | ----------- |
| ```String```      | transactionId      |

**Example Request**
```
Request 

POST /swap/estimate

{
   "swapClaimToken": "AgV4mhdSSNpB3KxejkfkSxU036JdfpMgzKiIcdOd4XAizBYAXwABABVhd3MtY3J5cHRvLXB1YmxpYy1rZXkAREF2WXNTLzhNejl6dVEwNnk1ZzQ1Wm1zblFTaHNyY0xXbzZQQW10YW0xQ1dnRTB6UTZyREZTeGVDU1Bjd0JGSk9mZz09AAEAB2F3cy1rbXMAS2Fybjphd3M6a21zOnVzLWVhc3QtMTo4NjEwNjA3MTQxMjU6a2V5L2JiZTJlNjNiLWRmYjQtNGI1NS1iNzdiLTEyZTc2NjZmN2I0YQC4AQIBAHhCZM0mN8+FmBmqT4/JZfwJsUzvBO6TKj/WSwbN4RsSUgEZyUN/tfqKuzf4MJOtvzrvAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMKoeof5JPXigu26AKAgEQgDuUrjP9ZCF+ijh5UWxUM/gR1f9WGpw4xZz7Xttx+jK3wT02/Eclai6pTc9zuqts+KixXB1NZvrgGxYWRAIAABAAvVh5CDrQ+pnqOUppN6WwR2PBmTLBl2x0ffS0HVnjGzHLI5C+DCU2F+fUnzaGCOy0/////wAAAAEAAAAAAAAAAAAAAAEAAACbiPfGEXGnzOTS5hTLumWa2x/oF3z7jk6dw8UDWAPvI1D7wcXFO++iIrpc9F6FVDVVOIy1mOUkKjUj6782Sb1Jta1avkTjKmnJq6fg1IRTjAK4PY+7UkImSxIQ5xZVhqEqJALhFOXp4mmckyS/lmAxNj7yh4PH7tDCYj1dc68cRFz6oUqDQTtLxSlYfWyA69rXLCBrusONsUXDsSkremGydx0Kl6eSaSfs01JYAGcwZQIwbkE4Fi/X8RjqHLPvSsQJkvWVP9zBX74IA7WtUmag/tLvj5UoX5neaEhT13D3NWP7AjEAzmHrVWkNm2dPBlrCvMYJ+sa4gqkj252NsJ3tdNcDxE8TbNhfHkcTQW6hskRM4VXh"
}

Response
{
    "transactionId": "81e3520f-bc19-4bbb-9ed4-fefedf9bc9cf"
}
```


## FAQ

### How does the AMM submit prices?

**Note:** In a real AMM, users would have the ability to set a *slippage* parameter that prevents actual swap to be too
far off from the initial estimate.

### How does the AMM maintain prices?
One standard way an AMM calculates price action is using the **constant product formula** - Whenever a
trade (swap of assets) is made in the pool, the market maker will **maintain a constant k-value**, which is calculated by:
```
k = marketCapAssetOne * marketCapAssetTwo
```
While maintaining this value, the AMM can set before / after prices. The logic for this algorithm is contained in `com.logic.MarketMakerLogic`.

## Project Setup
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
```
sls invoke --function createLiquidityPool --path apiTestData/createLiquidityPool.json
```

### GetLiquidityPool
```
sls invoke --function getLiquidityPool --path apiTestData/getLiquidityPool.json
```

### EstimateSwap

```
 sls invoke --function estimateSwap --path apiTestData/estimateSwap.json
```

### SubmitSwap

```
 sls invoke --function submitSwap --path apiTestData/submitSwap.json
```

* **Note:** You can append ```--log``` to any request to print logs in the lambda
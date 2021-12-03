# Automated Market Maker

An automated market maker is a type of market.

This project simulates an automated market maker by allowing users to create liquidity pools between arbitrary assets,
place trades / swaps between assets and observe price action.

## Example

## Learn
* 

## Setup
* Set up serverless CLI / AWS CLI
```
sls deploy
```

## APIs

**CreateLiquidityPool:**
```
sls invoke --function createLiquidityPool --path apiTestData/createLiquidityPool.json
```
**GetLiquidityPool:**
```
sls invoke --function getLiquidityPool --path apiTestData/getLiquidityPool.json
```
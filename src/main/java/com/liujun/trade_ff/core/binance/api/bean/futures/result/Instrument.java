package com.liujun.trade_ff.core.binance.api.bean.futures.result;

/**
 * futures contract products <br/>
 *
 * @author Tony Tian
 * @version 1.0.0
 * @date 2018/2/26 10:49
 */
public class Instrument {//合约参数

    /*  文档连接：https://binance-docs.github.io/apidocs/delivery/cn/#0f3f2d5ee7

            "symbol": "BTCUSD_200925", // 交易对
            "pair": "BTCUSD",   // 标的交易对
            "contractType": "CURRENT_QUARTER",   // 合约类型
            "deliveryDate": 1601020800000,
            "onboardDate": 1590739200000,
            "contractStatus": "TRADING", // 交易对状态
            "contractSize": 100,     //
            "quoteAsset": "USD", // 报价币种
            "baseAsset": "BTC",  // 标的物
            "marginAsset": "BTC",   // 保证金币种
            "pricePrecision": 1,   // 价格小数点位数(仅作为系统精度使用，注意同tickSize 区分)
            "quantityPrecision": 0, // 数量小数点位数(仅作为系统精度使用，注意同stepSize 区分)
            "baseAssetPrecision": 8,
            "quotePrecision": 8,
            "equalQtyPrecision": 4,     // 请忽略
            "triggerProtect": "0.0500", // 开启"priceProtect"的条件订单的触发阈值
            "maintMarginPercent": "2.5000", // 请忽略
            "requiredMarginPercent": "5.0000", // 请忽略
            "underlyingType": "COIN",  // 标的类型
            "underlyingSubType": []     // 标的物子类型
    */
    private String symbol;
    private String pair;
    private String contractType;
    private String contractStatus;
    private int contractSize;
    private String quoteAsset;
    private String baseAsset;
    private String marginAsset;
    private int pricePrecision;
    private int quantityPrecision;
    private int baseAssetPrecision;
    private int quotePrecision;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPair() {
        return pair;
    }

    public void setPair(String pair) {
        this.pair = pair;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public int getContractSize() {
        return contractSize;
    }

    public void setContractSize(int contractSize) {
        this.contractSize = contractSize;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public void setQuoteAsset(String quoteAsset) {
        this.quoteAsset = quoteAsset;
    }

    public String getBaseAsset() {
        return baseAsset;
    }

    public void setBaseAsset(String baseAsset) {
        this.baseAsset = baseAsset;
    }

    public String getMarginAsset() {
        return marginAsset;
    }

    public void setMarginAsset(String marginAsset) {
        this.marginAsset = marginAsset;
    }

    public int getPricePrecision() {
        return pricePrecision;
    }

    public void setPricePrecision(int pricePrecision) {
        this.pricePrecision = pricePrecision;
    }

    public int getQuantityPrecision() {
        return quantityPrecision;
    }

    public void setQuantityPrecision(int quantityPrecision) {
        this.quantityPrecision = quantityPrecision;
    }

    public int getBaseAssetPrecision() {
        return baseAssetPrecision;
    }

    public void setBaseAssetPrecision(int baseAssetPrecision) {
        this.baseAssetPrecision = baseAssetPrecision;
    }

    public int getQuotePrecision() {
        return quotePrecision;
    }

    public void setQuotePrecision(int quotePrecision) {
        this.quotePrecision = quotePrecision;
    }
}

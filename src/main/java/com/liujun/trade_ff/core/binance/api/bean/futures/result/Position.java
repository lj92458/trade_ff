package com.liujun.trade_ff.core.binance.api.bean.futures.result;

/**
 * 默认持仓模式为单向模式。若您想使用双向持仓模式，您需要在“设置”调整持仓模式
 * 对于单向持仓模式，只允许持有一个方向的仓位。"positions"仅会展示"BOTH"方向的持仓
 * 对于双向持仓模式，允许同时持有多单和空单。"positions"会展示所有"BOTH", "LONG", 和"SHORT"方向的持仓
 * BOTH: 单一持仓方向  LONG: 多头(双向持仓下)    SHORT: 空头(双向持仓下)
 */
public class Position {//仓位
    //保证金余额=钱包余额+浮盈浮亏
    //合约数量=(最新标标记价格 * 当前所需起始保证金)/合约面额
    private String symbol;// "BTCUSD_201225",  // 交易对
    private double initialMargin;// "22.9417118",   // 当前所需起始保证金(按最新标标记价格)
    private double maintMargin;// "0.29883423", // 持仓维持保证金
    private double unrealizedProfit;// "-0.81740553",  // 持仓未实现盈亏
    private double positionInitialMargin;// "22.9417118",  // 当前所需持仓起始保证金(按最新标标记价格)
    private double openOrderInitialMargin;// "0",  // 当前所需挂单起始保证金(按最新标标记价格)
    private int leverage;// "1",  // 杠杆倍率
    private boolean isolated;// false,  // 是否是逐仓模式
    private String positionSide;// BOTH: 单一持仓方向  LONG: 多头(双向持仓下)    SHORT: 空头(双向持仓下)
    private double entryPrice;// "57868.48112587",    // 平均持仓成本
    private long maxQty;// "9223372036854775807"  // 当前杠杆下最大可开仓数(标的数量)
    /*
    {
		"entryPrice": 57868.48112587,
		"initialMargin": 22.9417118,
		"isolated": false,
		"leverage": 1,
		"maintMargin": 0.29883423,
		"maxQty": 9223372036854775807,
		"openOrderInitialMargin": 0.0,
		"positionInitialMargin": 22.9417118,
		"positionSide": "BOTH",
		"symbol": "BTCUSD_210625",
		"unrealizedProfit": -0.81740553
	}
     */

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getInitialMargin() {
        return initialMargin;
    }

    public void setInitialMargin(double initialMargin) {
        this.initialMargin = initialMargin;
    }

    public double getMaintMargin() {
        return maintMargin;
    }

    public void setMaintMargin(double maintMargin) {
        this.maintMargin = maintMargin;
    }

    public double getUnrealizedProfit() {
        return unrealizedProfit;
    }

    public void setUnrealizedProfit(double unrealizedProfit) {
        this.unrealizedProfit = unrealizedProfit;
    }

    public double getPositionInitialMargin() {
        return positionInitialMargin;
    }

    public void setPositionInitialMargin(double positionInitialMargin) {
        this.positionInitialMargin = positionInitialMargin;
    }

    public double getOpenOrderInitialMargin() {
        return openOrderInitialMargin;
    }

    public void setOpenOrderInitialMargin(double openOrderInitialMargin) {
        this.openOrderInitialMargin = openOrderInitialMargin;
    }

    public int getLeverage() {
        return leverage;
    }

    public void setLeverage(int leverage) {
        this.leverage = leverage;
    }

    public boolean isIsolated() {
        return isolated;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(double entryPrice) {
        this.entryPrice = entryPrice;
    }

    public long getMaxQty() {
        return maxQty;
    }

    public void setMaxQty(long maxQty) {
        this.maxQty = maxQty;
    }
}

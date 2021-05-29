package com.liujun.trade_ff.core.binance.api.bean.futures.result;

public class Asset {//资产
    //保证金余额=钱包余额+浮盈浮亏=26.52748613+(-0.81740553)
    private String asset;// "BTC",  // 资产名
    private double walletBalance;// "26.52748613",  // 账户余额
    private double unrealizedProfit;// "-0.81740553",  // 全部持仓未实现盈亏
    private double marginBalance;// "25.7100806",  // 保证金余额
    private double maintMargin;// "0.29883423",    // 维持保证金
    private double initialMargin;// "22.9417118",  // 当前所需起始保证金(按最新标标记价格)
    private double positionInitialMargin;// "22.9417118",  // 当前所需持仓起始保证金(按最新标标记价格)
    private double openOrderInitialMargin;// "0.00000000",  // 当前所需挂单起始保证金(按最新标标记价格)
    private double maxWithdrawAmount;// "2.7683688",  // 最大可提款金额
    private double crossWalletBalance;// "26.52748613",  // 可用于全仓的账户余额
    private double crossUnPnl;// "-0.81740553",  // 所有全仓持仓的未实现盈亏
    private double availableBalance;// "2.7683688"  // 可用下单余额


    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public double getUnrealizedProfit() {
        return unrealizedProfit;
    }

    public void setUnrealizedProfit(double unrealizedProfit) {
        this.unrealizedProfit = unrealizedProfit;
    }

    public double getMarginBalance() {
        return marginBalance;
    }

    public void setMarginBalance(double marginBalance) {
        this.marginBalance = marginBalance;
    }

    public double getMaintMargin() {
        return maintMargin;
    }

    public void setMaintMargin(double maintMargin) {
        this.maintMargin = maintMargin;
    }

    public double getInitialMargin() {
        return initialMargin;
    }

    public void setInitialMargin(double initialMargin) {
        this.initialMargin = initialMargin;
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

    public double getMaxWithdrawAmount() {
        return maxWithdrawAmount;
    }

    public void setMaxWithdrawAmount(double maxWithdrawAmount) {
        this.maxWithdrawAmount = maxWithdrawAmount;
    }

    public double getCrossWalletBalance() {
        return crossWalletBalance;
    }

    public void setCrossWalletBalance(double crossWalletBalance) {
        this.crossWalletBalance = crossWalletBalance;
    }

    public double getCrossUnPnl() {
        return crossUnPnl;
    }

    public void setCrossUnPnl(double crossUnPnl) {
        this.crossUnPnl = crossUnPnl;
    }

    public double getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(double availableBalance) {
        this.availableBalance = availableBalance;
    }
}

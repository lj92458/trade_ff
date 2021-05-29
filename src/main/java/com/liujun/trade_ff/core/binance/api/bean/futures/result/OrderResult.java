package com.liujun.trade_ff.core.binance.api.bean.futures.result;

import com.liujun.trade_ff.core.binance.api.enums.*;

/**
 *
 */
public class OrderResult {
    private String clientOrderId;// "testOrder", // 用户自定义的订单号
    private int cumQty;// "0",
    private double cumBase;// "0", // 成交额(标的数量)
    private int executedQty;// "0", // 成交量(张数)
    private long orderId;// 22542179, // 系统订单号
    private double avgPrice;// "0.0",      // 平均成交价
    private int origQty;// "10", // 原始委托数量
    private double price;// "0", // 委托价格
    private boolean reduceOnly;// false, // 仅减仓
    private boolean closePosition;// false,   // 是否条件全平仓
    private OrderSide side;// "SELL", // 买卖方向
    private PositionSide positionSide;// "SHORT", // 持仓方向
    private OrderStatus status;// "NEW", // 订单状态
    private double stopPrice;// "0", // 触发价,对`TRAILING_STOP_MARKET`无效
    private String symbol;// "BTCUSD_200925", // 交易对
    private String pair;// "BTCUSD",   // 标的交易对
    private TimeInForce timeInForce;// "GTC", // 有效方法
    private OrderType type;// "TRAILING_STOP_MARKET", // 订单类型
    private OrderType origType;// "TRAILING_STOP_MARKET",  // 触发前订单类型
    private double activatePrice;// "9020", // 跟踪止损激活价格, 仅`TRAILING_STOP_MARKET` 订单返回此字段
    private double priceRate;// "0.3", // 跟踪止损回调比例, 仅`TRAILING_STOP_MARKET` 订单返回此字段
    private long updateTime;// 1566818724722, // 更新时间
    private WorkingType workingType;// "CONTRACT_PRICE", // 条件价格触发类型
    private boolean priceProtect;// false            // 是否开启条件单触发保护
    private long time;//订单时间

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public int getCumQty() {
        return cumQty;
    }

    public void setCumQty(int cumQty) {
        this.cumQty = cumQty;
    }

    public double getCumBase() {
        return cumBase;
    }

    public void setCumBase(double cumBase) {
        this.cumBase = cumBase;
    }

    public int getExecutedQty() {
        return executedQty;
    }

    public void setExecutedQty(int executedQty) {
        this.executedQty = executedQty;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public double getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(double avgPrice) {
        this.avgPrice = avgPrice;
    }

    public int getOrigQty() {
        return origQty;
    }

    public void setOrigQty(int origQty) {
        this.origQty = origQty;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isReduceOnly() {
        return reduceOnly;
    }

    public void setReduceOnly(boolean reduceOnly) {
        this.reduceOnly = reduceOnly;
    }

    public boolean isClosePosition() {
        return closePosition;
    }

    public void setClosePosition(boolean closePosition) {
        this.closePosition = closePosition;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public PositionSide getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(PositionSide positionSide) {
        this.positionSide = positionSide;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(double stopPrice) {
        this.stopPrice = stopPrice;
    }

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

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(TimeInForce timeInForce) {
        this.timeInForce = timeInForce;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public OrderType getOrigType() {
        return origType;
    }

    public void setOrigType(OrderType origType) {
        this.origType = origType;
    }

    public double getActivatePrice() {
        return activatePrice;
    }

    public void setActivatePrice(double activatePrice) {
        this.activatePrice = activatePrice;
    }

    public double getPriceRate() {
        return priceRate;
    }

    public void setPriceRate(double priceRate) {
        this.priceRate = priceRate;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public WorkingType getWorkingType() {
        return workingType;
    }

    public void setWorkingType(WorkingType workingType) {
        this.workingType = workingType;
    }

    public boolean isPriceProtect() {
        return priceProtect;
    }

    public void setPriceProtect(boolean priceProtect) {
        this.priceProtect = priceProtect;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

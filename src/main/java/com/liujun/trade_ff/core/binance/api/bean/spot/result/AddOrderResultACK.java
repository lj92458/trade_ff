package com.liujun.trade_ff.core.binance.api.bean.spot.result;

public class AddOrderResultACK {

    private String symbol;
    private long orderId;
    private long orderListId;
    private String clientOrderId;
    private long transactTime;

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderListId(long orderListId) {
        this.orderListId = orderListId;
    }

    public long getOrderListId() {
        return orderListId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setTransactTime(long transactTime) {
        this.transactTime = transactTime;
    }

    public long getTransactTime() {
        return transactTime;
    }
}

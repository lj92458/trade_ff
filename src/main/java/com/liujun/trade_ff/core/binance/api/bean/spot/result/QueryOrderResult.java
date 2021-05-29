package com.liujun.trade_ff.core.binance.api.bean.spot.result;

public class QueryOrderResult {
    private String symbol;
    private long orderId;
    private long orderListId;
    private String clientOrderId;
    private String price;
    private String origQty;
    private String executedQty;
    private String cummulativeQuoteQty;
    private String status;
    private String timeInForce;
    private String type;
    private String side;
    private String stopPrice;
    private String icebergQty;
    private long time;
    private long updateTime;
    private boolean isWorking;
    private String origQuoteOrderQty;
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

    public void setPrice(String price) {
        this.price = price;
    }
    public String getPrice() {
        return price;
    }

    public void setOrigQty(String origQty) {
        this.origQty = origQty;
    }
    public String getOrigQty() {
        return origQty;
    }

    public void setExecutedQty(String executedQty) {
        this.executedQty = executedQty;
    }
    public String getExecutedQty() {
        return executedQty;
    }

    public void setCummulativeQuoteQty(String cummulativeQuoteQty) {
        this.cummulativeQuoteQty = cummulativeQuoteQty;
    }
    public String getCummulativeQuoteQty() {
        return cummulativeQuoteQty;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }

    public void setTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }
    public String getTimeInForce() {
        return timeInForce;
    }

    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }

    public void setSide(String side) {
        this.side = side;
    }
    public String getSide() {
        return side;
    }

    public void setStopPrice(String stopPrice) {
        this.stopPrice = stopPrice;
    }
    public String getStopPrice() {
        return stopPrice;
    }

    public void setIcebergQty(String icebergQty) {
        this.icebergQty = icebergQty;
    }
    public String getIcebergQty() {
        return icebergQty;
    }

    public void setTime(long time) {
        this.time = time;
    }
    public long getTime() {
        return time;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    public long getUpdateTime() {
        return updateTime;
    }

    public void setIsWorking(boolean isWorking) {
        this.isWorking = isWorking;
    }
    public boolean getIsWorking() {
        return isWorking;
    }

    public void setOrigQuoteOrderQty(String origQuoteOrderQty) {
        this.origQuoteOrderQty = origQuoteOrderQty;
    }
    public String getOrigQuoteOrderQty() {
        return origQuoteOrderQty;
    }
}

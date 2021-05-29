package com.liujun.trade_ff.core.binance.api.bean.spot.param;

import com.liujun.trade_ff.core.binance.api.enums.NewOrderRespType;
import com.liujun.trade_ff.core.binance.api.enums.OrderSide;
import com.liujun.trade_ff.core.binance.api.enums.OrderType;
import com.liujun.trade_ff.core.binance.api.enums.TimeInForce;

public class PlaceOrderParam {
    String symbol;
    OrderSide side;
    OrderType type;
    TimeInForce timeInForce;
    double quantity;
    double quoteOrderQty;
    double price;
    String newClientOrderId;
    double stopPrice;
    double icebergQty;
    NewOrderRespType newOrderRespType;
    long recvWindow;
    long timestamp;


    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(TimeInForce timeInForce) {
        this.timeInForce = timeInForce;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getQuoteOrderQty() {
        return quoteOrderQty;
    }

    public void setQuoteOrderQty(double quoteOrderQty) {
        this.quoteOrderQty = quoteOrderQty;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getNewClientOrderId() {
        return newClientOrderId;
    }

    public void setNewClientOrderId(String newClientOrderId) {
        this.newClientOrderId = newClientOrderId;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(double stopPrice) {
        this.stopPrice = stopPrice;
    }

    public double getIcebergQty() {
        return icebergQty;
    }

    public void setIcebergQty(double icebergQty) {
        this.icebergQty = icebergQty;
    }

    public NewOrderRespType getNewOrderRespType() {
        return newOrderRespType;
    }

    public void setNewOrderRespType(NewOrderRespType newOrderRespType) {
        this.newOrderRespType = newOrderRespType;
    }

    public long getRecvWindow() {
        return recvWindow;
    }

    public void setRecvWindow(long recvWindow) {
        this.recvWindow = recvWindow;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

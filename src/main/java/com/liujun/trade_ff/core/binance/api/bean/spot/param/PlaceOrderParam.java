package com.liujun.trade_ff.core.binance.api.bean.spot.param;

import com.liujun.trade_ff.core.binance.api.enums.NewOrderRespType;
import com.liujun.trade_ff.core.binance.api.enums.OrderSide;
import com.liujun.trade_ff.core.binance.api.enums.OrderType;
import com.liujun.trade_ff.core.binance.api.enums.TimeInForce;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;

public class PlaceOrderParam {
    String symbol;
    OrderSide side;
    OrderType type;
    TimeInForce timeInForce;
    Double quantity;
    Double quoteOrderQty;//quoteOrderQty=100:下买单的时候, 订单会尽可能的买进价值100USDT的BTC.下卖单的时候, 订单会尽可能的卖出价值100USDT的BTC.
    Double price;
    String newClientOrderId;
    Double stopPrice;
    Double icebergQty;
    NewOrderRespType newOrderRespType;
    long recvWindow;
    Long timestamp = DateUtils.getUnixTimeMilli();

    public PlaceOrderParam(long recvWindow, long timestampAdd) {
        this.recvWindow = recvWindow;
        this.timestamp += timestampAdd;
    }

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

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getQuoteOrderQty() {
        return quoteOrderQty;
    }

    public void setQuoteOrderQty(Double quoteOrderQty) {
        this.quoteOrderQty = quoteOrderQty;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getNewClientOrderId() {
        return newClientOrderId;
    }

    public void setNewClientOrderId(String newClientOrderId) {
        this.newClientOrderId = newClientOrderId;
    }

    public Double getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(Double stopPrice) {
        this.stopPrice = stopPrice;
    }

    public Double getIcebergQty() {
        return icebergQty;
    }

    public void setIcebergQty(Double icebergQty) {
        this.icebergQty = icebergQty;
    }

    public NewOrderRespType getNewOrderRespType() {
        return newOrderRespType;
    }

    public void setNewOrderRespType(NewOrderRespType newOrderRespType) {
        this.newOrderRespType = newOrderRespType;
    }

    public Long getRecvWindow() {
        return recvWindow;
    }

    public void setRecvWindow(Long recvWindow) {
        this.recvWindow = recvWindow;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

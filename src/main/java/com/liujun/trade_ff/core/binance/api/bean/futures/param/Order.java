package com.liujun.trade_ff.core.binance.api.bean.futures.param;

import com.liujun.trade_ff.core.binance.api.enums.*;

/**
 * 下单
 */
public class Order {
    private String symbol;//	STRING	YES	交易对
    private OrderSide side;//	ENUM	YES	买卖方向 SELL, BUY
    private PositionSide positionSide;//	ENUM	NO	持仓方向,单向持仓模式下非必填,默认且仅可填BOTH;在双向持仓模式下必填,且仅可选择 LONG 或 SHORT
    private OrderType type;//	ENUM	YES	订单类型 LIMIT, MARKET, STOP, TAKE_PROFIT, STOP_MARKET, TAKE_PROFIT_MARKET, TRAILING_STOP_MARKET
    private String reduceOnly;//	STRING	NO	true, false; 非双开模式下默认false；双开模式下不接受此参数； 使用closePosition不支持此参数。
    private int quantity;//	DECIMAL	NO	下单数量,使用closePosition不支持此参数。
    private double price;//	DECIMAL	NO	委托价格
    private String newClientOrderId;//	STRING	NO	用户自定义的订单号,不可以重复出现在挂单中。如空缺系统会自动赋值。必须满足正则规则 ^[\.A-Z\:/a-z0-9_-]{1,36}$
    private double stopPrice;//	DECIMAL	NO	触发价, 仅 STOP, STOP_MARKET, TAKE_PROFIT, TAKE_PROFIT_MARKET 需要此参数
    private String closePosition;//	STRING	NO	true, false；触发后全部平仓,仅支持STOP_MARKET和TAKE_PROFIT_MARKET；不与quantity合用；自带只平仓效果,不与reduceOnly 合用
    private double activationPrice;//	DECIMAL	NO	追踪止损激活价格,仅TRAILING_STOP_MARKET 需要此参数, 默认为下单当前市场价格(支持不同workingType)
    private double callbackRate;//	DECIMAL	NO	追踪止损回调比例,可取值范围[0.1, 4],其中 1代表1% ,仅TRAILING_STOP_MARKET 需要此参数
    private TimeInForce timeInForce;//	ENUM	NO	有效方法
    private WorkingType workingType;//	ENUM	NO	stopPrice 触发类型: MARK_PRICE(标记价格), CONTRACT_PRICE(合约最新价). 默认 CONTRACT_PRICE
    private String priceProtect;//	STRING	NO	条件单触发保护："TRUE","FALSE", 默认"FALSE". 仅 STOP, STOP_MARKET, TAKE_PROFIT, TAKE_PROFIT_MARKET 需要此参数
    private NewOrderRespType newOrderRespType;//	ENUM	NO	"ACK", "RESULT", 默认 "ACK"
    private long recvWindow;//	LONG	NO
    private long timestamp;//	LONG	YES

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

    public PositionSide getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(PositionSide positionSide) {
        this.positionSide = positionSide;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public String getReduceOnly() {
        return reduceOnly;
    }

    public void setReduceOnly(String reduceOnly) {
        this.reduceOnly = reduceOnly;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
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

    public String getClosePosition() {
        return closePosition;
    }

    public void setClosePosition(String closePosition) {
        this.closePosition = closePosition;
    }

    public double getActivationPrice() {
        return activationPrice;
    }

    public void setActivationPrice(double activationPrice) {
        this.activationPrice = activationPrice;
    }

    public double getCallbackRate() {
        return callbackRate;
    }

    public void setCallbackRate(double callbackRate) {
        this.callbackRate = callbackRate;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(TimeInForce timeInForce) {
        this.timeInForce = timeInForce;
    }

    public WorkingType getWorkingType() {
        return workingType;
    }

    public void setWorkingType(WorkingType workingType) {
        this.workingType = workingType;
    }

    public String getPriceProtect() {
        return priceProtect;
    }

    public void setPriceProtect(String priceProtect) {
        this.priceProtect = priceProtect;
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

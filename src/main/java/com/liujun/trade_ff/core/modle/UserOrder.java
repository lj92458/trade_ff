package com.liujun.trade_ff.core.modle;

import com.liujun.trade_ff.core.Prop;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 程序自己将要挂的单
 */
public class UserOrder {
    private int platId;
    /**
     * 跟该订单配对的那个订单。(订单都是成双成对的,一买一卖,要么一起成功,要么一起失败)
     */
    private UserOrder anotherOrder;
    /**
     * 该订单是否生效?(如果让该订单失效,那么它的配对订单也应该失效)
     */
    private boolean enable = true;
    /**
     * 类型：buy,sell
     */
    private String type;
    /**
     * 价格
     */
    private double price;
    /**
     * 能赚取的差价
     */
    private double diffPrice;
    /**
     * 数量
     */
    private double volume;
    /**
     * 挂单后,返回的id
     */
    private String orderId;
    /**
     * 是否完全成交
     */
    private boolean finished = false;



    public UserOrder() {

    }

    /**
     * 让该订单和它的配对订单都失效。
     */
    public void disableOrder() {
        this.setEnable(false);
        if (anotherOrder != null) {
            anotherOrder.setEnable(false);
        }

    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {

        this.price=price;
    }

    public double getDiffPrice() {
        return diffPrice;
    }

    public void setDiffPrice(double diffPrice) {
        this.diffPrice = diffPrice;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {

        this.volume=volume;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public UserOrder getAnotherOrder() {
        return anotherOrder;
    }

    public void setAnotherOrder(UserOrder anotherOrder) {
        this.anotherOrder = anotherOrder;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * 设置了自己的量,也要设置对方的量
     */
    public void changeVolume(double volume) {
        setVolume(volume);
        getAnotherOrder().setVolume(volume);
    }

    public int getPlatId() {
        return platId;
    }

    public void setPlatId(int platId) {
        this.platId = platId;
    }

    @Override
    public String toString() {
        return "{price:" + price + ",amount:" + volume + ",type:'" + type + "'}";
    }

}

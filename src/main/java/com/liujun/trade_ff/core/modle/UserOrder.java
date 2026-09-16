package com.liujun.trade_ff.core.modle;

import com.liujun.trade_ff.core.Prop;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 程序自己将要挂的单
 */
@Getter
@Setter
public class UserOrder {
    private int platId;
    /**
     * 跟该订单配对的那个订单。(订单都是成双成对的,一买一卖,要么一起成功,要么一起失败)
     */
    private UserOrder anotherOrder;//这个属性不能参与toString()方法，否则是互相引用，死循环
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

    private Integer nonce;//dex平台特有的，用来取消正在排队的交易


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

    /**
     * 设置了自己的量,也要设置对方的量
     */
    public void changeVolume(double volume) {
        setVolume(volume);
        getAnotherOrder().setVolume(volume);
    }

    @Override
    public String toString() {
        return "{price:" + price + ",amount:" + volume + ",type:'" + type + ",orderId:'" + orderId + "'}";
    }


}

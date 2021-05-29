package com.liujun.trade_ff.core.modle;

import com.liujun.trade_ff.core.Prop;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 市场挂单
 *
 * @author Administrator
 */
public class MarketOrder implements Comparable<MarketOrder>, Cloneable {
    /**
     * 哪个平台
     */
    private int platId;

    /**
     * 价格
     */
    private double price;
    /**
     * 数量
     */
    private double volume;


    public int getPlatId() {
        return platId;
    }

    public void setPlatId(int platId) {
        this.platId = platId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {

        this.price=price;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {

        this.volume=volume;
    }

    public int compareTo(MarketOrder arg0) {
        if (price < arg0.price) {
            return -1;
        } else if (price > arg0.price) {
            return 1;
        } else {
            return 0;
        }
    }

    public String toString() {

        return "{price:" + price + ",volume:" + volume + ",platId:" + platId + "}";
    }

    public MarketOrder clone() {
        try {
            return (MarketOrder) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

}

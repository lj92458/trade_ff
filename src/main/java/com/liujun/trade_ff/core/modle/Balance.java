package com.liujun.trade_ff.core.modle;

import com.liujun.trade_ff.core.Prop;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Balance {

    /**
     * 2015-03-19 20:00:00
     */
    private String dateTime;
    private double totalEarn;
    private double thisEarn;
    private double totalMoney;
    private double totalGoods;
    private String platInfo;
    private double price;
    Prop prop;


    private DecimalFormat fmt_goods;
    private DecimalFormat fmt_money;

    private void init() {
        fmt_goods = new DecimalFormat(prop.formatGoodsStr);
        fmt_money = new DecimalFormat(prop.formatMoneyStr);
        fmt_goods.setRoundingMode(RoundingMode.HALF_UP);
        fmt_money.setRoundingMode(RoundingMode.HALF_UP);

    }

    public Balance(Prop prop) {
        this.prop = prop;
        init();
    }

    /**
     * 从字符串解析成对象
     */
    public Balance(Prop prop, String balanceStr) {
        this.prop = prop;
        init();
        int beginIndex;
        String strValue;
        beginIndex = balanceStr.indexOf("dateTime:") + "dateTime:".length();
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf(",", beginIndex));
        setDateTime(strValue);
        //
        beginIndex = balanceStr.indexOf("totalEarn:") + "totalEarn:".length();
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf(",", beginIndex));
        setTotalEarn(Double.parseDouble(strValue));
        //
        beginIndex = balanceStr.indexOf("thisEarn:") + "thisEarn:".length();
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf(",", beginIndex));
        setThisEarn(Double.parseDouble(strValue));
        //
        beginIndex = balanceStr.indexOf("totalMoney:") + "totalMoney:".length();
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf(",", beginIndex));
        setTotalMoney(Double.parseDouble(strValue));
        //
        beginIndex = balanceStr.indexOf("totalGoods:") + "totalGoods:".length();
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf(",", beginIndex));
        setTotalGoods(Double.parseDouble(strValue));
        //
        beginIndex = balanceStr.indexOf("{") + 1;
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf("}", beginIndex));
        setPlatInfo(strValue);
        //
        //
        beginIndex = balanceStr.indexOf("price:") + "price:".length();
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf(",", beginIndex));
        setPrice(Double.parseDouble(strValue));

    }

    // end static =====================

    public String toString() {

        return "dateTime:" + dateTime + ",totalEarn:" + totalEarn + ",thisEarn:" + thisEarn + ",totalMoney:" + totalMoney + ",totalGoods:" + totalGoods + ",{" + platInfo + "},price:" + price + ",";
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public double getTotalEarn() {
        return totalEarn;
    }

    public void setTotalEarn(double totalEarn) {
        if (prop.earnMoney) {
            synchronized (fmt_money) {
                this.totalEarn = Double.parseDouble(fmt_money.format(totalEarn));
            }
        } else {
            synchronized (fmt_goods) {
                //System.out.println("totalEarn"+totalEarn);
                //System.out.println(fmt_goods.format(totalEarn));
                this.totalEarn = Double.parseDouble(fmt_goods.format(totalEarn));
            }
        }
    }

    public double getThisEarn() {
        return thisEarn;
    }

    public void setThisEarn(double thisEarn) {
        if (prop.earnMoney) {
            synchronized (fmt_money) {
                this.thisEarn = Double.parseDouble(fmt_money.format(thisEarn));
            }
        } else {
            synchronized (fmt_goods) {
                this.thisEarn = Double.parseDouble(fmt_goods.format(thisEarn));
            }
        }
    }

    public double getTotalMoney() {
        return totalMoney;
    }

    public void setTotalMoney(double totalMoney) {
        synchronized (fmt_money) {
            this.totalMoney = Double.parseDouble(fmt_money.format(totalMoney));
        }
    }

    public double getTotalGoods() {
        return totalGoods;
    }

    public void setTotalGoods(double totalGoods) {
        synchronized (fmt_goods) {
            this.totalGoods = Double.parseDouble(fmt_goods.format(totalGoods));
        }
    }

    public String getPlatInfo() {
        return platInfo;
    }

    public void setPlatInfo(String platInfo) {
        this.platInfo = platInfo;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        synchronized (fmt_money) {
            this.price = Double.parseDouble(fmt_money.format(price));
        }
    }

}

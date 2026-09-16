package com.liujun.trade_ff.core.modle;

import com.liujun.trade_ff.core.Prop;
import lombok.Getter;
import lombok.Setter;

import java.math.RoundingMode;
import java.text.DecimalFormat;

@Getter
@Setter
public class Balance {

    /**
     * 2015-03-19 20:00:00
     */
    private String dateTime;
    private double totalEarn;
    private double thisEarn;
    public double[] totalToken = new double[2];
    private String platInfo;
    private double price;
    Prop prop;


    private DecimalFormat fmt_goods;
    private DecimalFormat fmt_money;

    private void init() {
        fmt_goods = new DecimalFormat(prop.formatGoodsStr);
        fmt_money = new DecimalFormat(prop.formatMoneyStr);
        fmt_goods.setRoundingMode(RoundingMode.DOWN);
        fmt_money.setRoundingMode(RoundingMode.DOWN);

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
        totalToken[1] = Double.parseDouble(strValue);
        //
        beginIndex = balanceStr.indexOf("totalGoods:") + "totalGoods:".length();
        strValue = balanceStr.substring(beginIndex, balanceStr.indexOf(",", beginIndex));
        totalToken[0] = Double.parseDouble(strValue);
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

        return "dateTime:" + dateTime + ",totalEarn:" + totalEarn + ",thisEarn:" + thisEarn + ",totalMoney:" + totalToken[1] + ",totalGoods:" + totalToken[0] + ",{" + platInfo + "},price:" + price + ",";
    }

    public void setTotalEarn(double totalEarn) {
        if (prop.earnMoney) {
            synchronized (fmt_money) {
                this.totalEarn = Double.parseDouble(fmt_money.format(totalEarn));
            }
        } else {
            synchronized (fmt_goods) {
                this.totalEarn = Double.parseDouble(fmt_goods.format(totalEarn));
            }
        }
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


    public void setPrice(double price) {
        synchronized (fmt_money) {
            this.price = Double.parseDouble(fmt_money.format(price));
        }
    }

}

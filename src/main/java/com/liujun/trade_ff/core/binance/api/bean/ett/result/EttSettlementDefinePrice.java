package com.liujun.trade_ff.core.binance.api.bean.ett.result;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author chuping.cui
 * @date 2018/7/5
 */
public class EttSettlementDefinePrice {
    private String date;
    private BigDecimal price;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        com.liujun.trade_ff.core.binance.api.bean.ett.result.EttSettlementDefinePrice that = (com.liujun.trade_ff.core.binance.api.bean.ett.result.EttSettlementDefinePrice)o;
        return Objects.equals(date, that.date) &&
            Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, price);
    }

    @Override
    public String toString() {
        return "EttSettlementDefinePrice{" +
            "date=" + date +
            ", price=" + price +
            '}';
    }
}

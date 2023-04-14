package com.liujun.trade_ff.core.uniswap.api.bean;

public class Account {
    private String currency;
    /** 活动资金、闲散资金 */
    private String available;
    /** 挂单被占用资金、冻结资金 */
    private String hold;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }

    public String getHold() {
        return hold;
    }

    public void setHold(String hold) {
        this.hold = hold;
    }

    @Override
    public String toString() {
        return "{currency:\"" + currency + "\",available:\"" + available + "\",hold:\"" + hold + "\"}";
    }
}

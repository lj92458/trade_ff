package com.liujun.trade_ff.core.uniswap.api.bean;

public class Account {
    private String currency;
    private String available;
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

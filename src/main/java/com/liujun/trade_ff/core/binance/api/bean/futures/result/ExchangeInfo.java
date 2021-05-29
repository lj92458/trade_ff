package com.liujun.trade_ff.core.binance.api.bean.futures.result;

import java.util.List;

/**
 * 获取交易规则和交易对  https://binance-docs.github.io/apidocs/delivery/cn/#3f1907847c
 */
public class ExchangeInfo {
   private long serverTime;
    private String timezone;//UTC
    private List<Instrument> symbols;


    //--- getter and setter-------------------
    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<Instrument> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<Instrument> symbols) {
        this.symbols = symbols;
    }
}

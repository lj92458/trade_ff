package com.liujun.trade_ff.core.binance.api.enums;

public enum OrderType {
    LIMIT,// 限价单
    MARKET,// 市价单
    STOP_LOSS,//止损单
    STOP_LOSS_LIMIT,//限价止损单
    TAKE_PROFIT,// 止盈单
    TAKE_PROFIT_LIMIT,// 限价止盈单
    LIMIT_MAKER,// 限价卖单
    //以下是合约专有的
    STOP,// 止损限价单
    STOP_MARKET,// 止损市价单
    TAKE_PROFIT_MARKET,// 止盈市价单
    TRAILING_STOP_MARKET,// 跟踪止损单
}

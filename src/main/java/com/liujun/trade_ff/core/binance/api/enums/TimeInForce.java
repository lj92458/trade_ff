package com.liujun.trade_ff.core.binance.api.enums;

/**
 * 有效方式
 */
public enum TimeInForce {
    GTC,// 成交为止
    IOC,// 无法立即成交的部分就撤销
    FOK,// 无法全部立即成交就撤销
    GTX// - Good Till Crossing 无法成为挂单方就撤销
}

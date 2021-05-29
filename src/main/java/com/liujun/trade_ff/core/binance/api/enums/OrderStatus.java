package com.liujun.trade_ff.core.binance.api.enums;

public enum OrderStatus {

    NEW,//新建订单
    PARTIALLY_FILLED,//部分成交
    FILLED,// 全部成交
    CANCELED,//已撤销
    PENDING_CANCEL,// 撤销中（目前并未使用）
    REJECTED,// 订单被拒绝
    EXPIRED,// 订单过期（根据timeInForce参数规则）
}

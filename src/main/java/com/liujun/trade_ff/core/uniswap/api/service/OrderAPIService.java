package com.liujun.trade_ff.core.uniswap.api.service;

import com.liujun.trade_ff.core.uniswap.api.bean.AddOrderResult;

public interface OrderAPIService {

    /**
     * 挂单。
     * @param coinPair 格式：goods-money，例如：eth-usdc
     * @param orderType buy,sell
     * @param price
     * @param volume
     * @param slippage
     * @return
     */
    AddOrderResult addOrder(String coinPair, String orderType, String price, String volume, String gasPriceGwei, double slippage);

    //返回status
    String queryOrder(String coinPair, String orderId);

    void cancelOrder(String coinPair, String orderId);
}

package com.liujun.trade_ff.core.oneInch.api.rpc;

import com.liujun.trade_ff.core.uniswap.api.bean.TransResult;
import hprose.util.concurrent.Promise;

public interface OrderRpc {
    Promise<TransResult> addOrderOneInch(String coinPair, String orderType, String price, String volume, int maxWaitSeconds, String gasPriceGwei, double slippage);

    Promise<TransResult> addTwoOrderOneInch(String coinPair1, String orderType1, String price1, String volume1, int maxWaitSeconds1, String gasPriceGwei1, double slippage1,
                                            String coinPair2, String orderType2, String price2, String volume2, int maxWaitSeconds2, String gasPriceGwei2, double slippage2);
}

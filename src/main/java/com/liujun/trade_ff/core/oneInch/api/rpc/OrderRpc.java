package com.liujun.trade_ff.core.oneInch.api.rpc;

import com.liujun.trade_ff.core.uniswap.api.bean.TransResult;
import hprose.util.concurrent.Promise;

public interface OrderRpc {
    Promise<TransResult> addOrderOneInch(String coinPair, String orderType, String price, String volume, int maxWaitSeconds, String gasPriceGwei, double slippage);
}

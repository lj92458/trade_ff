package com.liujun.trade_ff.core.uniswap.api.rpc;

import com.liujun.trade_ff.core.uniswap.api.bean.AddOrderResult;
import hprose.util.concurrent.Promise;

public interface OrderRpc {
    Promise<AddOrderResult> addOrder(String coinPair, String orderType, String price, String volume, int maxWaitSeconds, String gasPriceGwei, double slippage,int poolFee);
}

package com.liujun.trade_ff.core.uniswap.api.service;

import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawParam;
import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawResult;
import hprose.util.concurrent.Promise;

public interface WalletAPIService {
    /**
     * 提币
     * @param param
     * @param gasPriceGwei
     * @return
     * @throws Exception
     */
    WithdrawResult withdraw(WithdrawParam param, double gasPriceGwei) throws Exception;

    Integer receiveToken(String asset, String txId, double amount, boolean needWrap, double gasPriceGwei);
}

package com.liujun.trade_ff.core.uniswap.api.service;

import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawParam;
import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawResult;

public interface WalletAPIService {
    WithdrawResult withdraw(WithdrawParam param) throws Exception;
}

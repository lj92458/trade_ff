package com.liujun.trade_ff.core.uniswap.api.service.impl;

import com.liujun.trade_ff.core.uniswap.api.bean.APIConfiguration;
import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawParam;
import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawResult;
import com.liujun.trade_ff.core.uniswap.api.service.WalletAPIService;

public class WalletAPIServiceImpl implements WalletAPIService {

    APIConfiguration config;

    public WalletAPIServiceImpl(APIConfiguration config) {
        this.config=config;
    }


    @Override
    public WithdrawResult withdraw(WithdrawParam param) throws Exception {
        return null;
    }
}

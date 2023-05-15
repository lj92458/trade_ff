package com.liujun.trade_ff.core.uniswap.api.service.impl;

import com.liujun.trade_ff.core.uniswap.api.bean.*;
import com.liujun.trade_ff.core.uniswap.api.rpc.AccountRpc;
import com.liujun.trade_ff.core.uniswap.api.rpc.RpcClient;
import com.liujun.trade_ff.core.uniswap.api.service.WalletAPIService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WalletAPIServiceImpl implements WalletAPIService {

    APIConfiguration config;
    AccountRpc accountRpc;

    public WalletAPIServiceImpl(APIConfiguration config) {
        this.config = config;
        this.accountRpc = RpcClient.getInstance(config.getUri()).useService(AccountRpc.class);
    }


    @Override
    public WithdrawResult withdraw(WithdrawParam param, double gasPriceGwei) throws Exception {
        try {
            TransResult transResult = accountRpc.sendToken(param.getAsset(), param.getAddress(), param.getAmount(), param.isNeedWrap(), config.getMaxWaitSeconds(), gasPriceGwei).toFuture().get(config.getMaxWaitSeconds(), TimeUnit.SECONDS);

            return new WithdrawResult(null, true, transResult.getHash());
        } catch (Exception e) {
            log.error("withdraw异常", e);
            throw new RuntimeException(e);

        }

    }

    @Override
    public double receiveToken(String asset, String txId, double amount, boolean needWrap, double gasPriceGwei) {
        try {
            return accountRpc.receiveToken(asset, txId, amount, needWrap, config.getMaxWaitSeconds(), gasPriceGwei)
                    .toFuture().get(config.getMaxWaitSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("receiveToken异常", e);
            throw new RuntimeException(e);
        }
    }
}

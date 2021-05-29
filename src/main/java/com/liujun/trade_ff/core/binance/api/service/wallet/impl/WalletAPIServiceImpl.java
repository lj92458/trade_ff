package com.liujun.trade_ff.core.binance.api.service.wallet.impl;

import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.wallet.WalletAPIService;
import org.apache.commons.beanutils.PropertyUtils;

public class WalletAPIServiceImpl implements WalletAPIService {

    private APIClient client;
    private WalletAPI api;

    public WalletAPIServiceImpl(APIConfiguration config) {
        this.client = new APIClient(config);
        this.api = client.createService(WalletAPI.class);
    }


    @Override
    public WithdrawResult withdraw(WithdrawParam param) throws Exception{
        return this.client.executeSync(this.api.withdraw(PropertyUtils.describe(param)));
    }
}

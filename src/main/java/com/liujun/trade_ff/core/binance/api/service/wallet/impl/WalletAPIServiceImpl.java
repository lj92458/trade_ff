package com.liujun.trade_ff.core.binance.api.service.wallet.impl;

import com.alibaba.fastjson2.JSON;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.DepositQueryParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawQueryParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.DepositQueryResult;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawQueryResult;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.wallet.WalletAPIService;
import com.liujun.trade_ff.core.binance.api.utils.MapUtil;

public class WalletAPIServiceImpl implements WalletAPIService {

    private APIClient client;
    private WalletAPI api;

    public WalletAPIServiceImpl(APIConfiguration config) {
        this.client = new APIClient(config);
        this.api = client.createService(WalletAPI.class);
    }


    @Override
    public WithdrawResult withdraw(WithdrawParam param) throws Exception {
        return this.client.executeSync(this.api.withdraw(MapUtil.toMapWithoutNullField(param)));
    }

    @Override
    public WithdrawQueryResult withdrawQuery(WithdrawQueryParam param) throws Exception {
        return this.client.executeSync(this.api.withdrawQuery(MapUtil.toMapWithoutNullField(param)));
    }

    /**
     * 查询所有币的信息。能看出它们分别支持什么网络，以及网络名称
     *
     * @return
     */
    public String queryAllCoin(long timestamp) {
        return this.client.executeSync(this.api.queryAllCoin(timestamp));
    }

    @Override
    public DepositQueryResult depositQuery(DepositQueryParam param) throws Exception {
        DepositQueryResult[] arr = this.client.executeSync(this.api.depositQuery(MapUtil.toMapWithoutNullField(param)));
        if (arr.length > 0) {
            return arr[0];
        } else {
            return null;
        }

    }

    public long queryTime() {
        return JSON.parseObject(this.client.executeSync(this.api.queryTime())).getLong("serverTime");

    }
}

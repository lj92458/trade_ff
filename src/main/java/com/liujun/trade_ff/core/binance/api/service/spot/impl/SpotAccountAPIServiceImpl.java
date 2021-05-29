package com.liujun.trade_ff.core.binance.api.service.spot.impl;

import com.liujun.trade_ff.core.binance.api.bean.spot.result.Account;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.spot.SpotAccountAPIService;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;

public class SpotAccountAPIServiceImpl implements SpotAccountAPIService {

    private final APIClient client;
    private final SpotAccountAPI api;

    public SpotAccountAPIServiceImpl(final APIConfiguration config) {
        this.client = new APIClient(config);
        this.api = this.client.createService(SpotAccountAPI.class);
    }

    @Override
    public Account accountInfo(long recvWindow) {
        return this.client.executeSync(this.api.accountInfo(recvWindow, DateUtils.getUnixTimeMilli()));
    }


}

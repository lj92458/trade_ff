package com.liujun.trade_ff.core.binance.api.service.spot.impl;

import com.liujun.trade_ff.core.binance.api.bean.spot.result.Account;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.Depth;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.spot.SpotProductAPIService;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;

/**
 * 公共数据相关接口
 **/
public class SpotProductAPIServiceImpl implements SpotProductAPIService {

    private final APIClient client;
    private final SpotProductAPI spotProductAPI;

    public SpotProductAPIServiceImpl(final APIConfiguration config) {
        this.client = new APIClient(config);
        this.spotProductAPI = this.client.createService(SpotProductAPI.class);
    }


    @Override
    public Depth marketDepth(String symbol, Integer limit) {

        return this.client.executeSync(this.spotProductAPI.marketDepth(symbol, limit));
    }




}

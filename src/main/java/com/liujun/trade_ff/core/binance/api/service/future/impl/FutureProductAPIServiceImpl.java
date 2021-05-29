package com.liujun.trade_ff.core.binance.api.service.future.impl;

import com.liujun.trade_ff.core.binance.api.bean.futures.result.ExchangeInfo;
import com.liujun.trade_ff.core.binance.api.bean.futures.result.Instrument;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.Depth;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.future.FutureProductAPIService;

import java.util.List;

public class FutureProductAPIServiceImpl implements FutureProductAPIService {
    private final APIClient client;
    private final FutureProductAPI futureProductAPI;

    public FutureProductAPIServiceImpl(APIConfiguration config) {
        this.client = new APIClient(config);
        this.futureProductAPI = this.client.createService(FutureProductAPI.class);
    }

    @Override
    public Depth marketDepth(String symbol, Integer limit) {
        return this.client.executeSync(this.futureProductAPI.marketDepth(symbol, limit));
    }

    @Override
    public List<Instrument> getInstruments() {
        ExchangeInfo info = this.client.executeSync(this.futureProductAPI.getInstruments());
        return info.getSymbols();
    }

    @Override
    public double lastMarkPrice(String symbol) {
        //返回数据：[[1619512500000,"56822.94576616","56832.47415389","56725.65549242","56745.18224293","0",1619512559999,"0",59,"0","0","0"]]
        Object price = this.client.executeSync(this.futureProductAPI.markPriceKlines(symbol, "1m", 1))[0][4];
        return Double.parseDouble((String) price);
    }
}

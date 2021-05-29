package com.liujun.trade_ff.core.binance.api.service.future.impl;

import com.liujun.trade_ff.core.binance.api.bean.futures.param.Order;
import com.liujun.trade_ff.core.binance.api.bean.futures.result.OrderResult;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.future.FutureOrderAPIService;
import com.liujun.trade_ff.core.binance.api.utils.MapUtil;
import org.apache.commons.beanutils.PropertyUtils;

public class FutureOrderAPIServiceImpl implements FutureOrderAPIService {
    private final APIClient client;
    private final FutureOrderAPI futureOrderAPI;
    public FutureOrderAPIServiceImpl(APIConfiguration config) {
        this.client = new APIClient(config);
        this.futureOrderAPI=this.client.createService(FutureOrderAPI.class);
    }

    @Override
    public OrderResult addOrder(Order order) throws Exception{

        return this.client.executeSync(futureOrderAPI.addOrder(MapUtil.toMap(order)));
    }

    @Override
    public OrderResult queryOrder(String symbol, long orderId, String origClientOrderId, long recvWindow, long timestamp) {
        return null;
    }

    @Override
    public OrderResult cancelOrder(String symbol, long orderId, String origClientOrderId, long recvWindow, long timestamp) {
        return null;
    }
}

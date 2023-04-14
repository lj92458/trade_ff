package com.liujun.trade_ff.core.binance.api.service.spot.impl;

import com.liujun.trade_ff.core.binance.api.bean.spot.param.PlaceOrderParam;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.AddOrderResultACK;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.CancelOrderResult;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.QueryOrderResult;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.enums.NewOrderRespType;
import com.liujun.trade_ff.core.binance.api.service.spot.SpotOrderAPIService;
import org.apache.commons.beanutils.PropertyUtils;


/**
 * 币币订单相关接口
 **/
public class SpotOrderAPIServiceImpl implements SpotOrderAPIService {
    private final APIClient client;
    private final SpotOrderAPI spotOrderAPI;

    public SpotOrderAPIServiceImpl(final APIConfiguration config) {
        this.client = new APIClient(config);
        this.spotOrderAPI = this.client.createService(SpotOrderAPI.class);
    }


    /*
       symbol, orderSide, orderType, DateUtils.getUnixTimeMilli(),
                timeInForce, quantity, price, recvWindow, NewOrderRespType.ACK
        */
    @Override
    public AddOrderResultACK addOrderACK(PlaceOrderParam param) throws Exception {

        param.setNewOrderRespType(NewOrderRespType.ACK);// NewOrderRespType.ACK

        return (AddOrderResultACK)this.client.executeSync(spotOrderAPI.addOrderACK(PropertyUtils.describe(param)));
    }

    @Override
    public QueryOrderResult queryOrder(String symbol, long orderId, String origClientOrderId,
                                       long recvWindow, long timestamp) {
        return this.client.executeSync(spotOrderAPI.queryOrder(
                symbol, orderId, origClientOrderId, recvWindow, timestamp));
    }

    @Override
    public CancelOrderResult cancelOrder(String symbol, long orderId, String origClientOrderId, String newClientOrderId, long recvWindow, long timestamp) {
        return this.client.executeSync(spotOrderAPI.cancelOrder(symbol,orderId,origClientOrderId,newClientOrderId,recvWindow,timestamp));
    }
}

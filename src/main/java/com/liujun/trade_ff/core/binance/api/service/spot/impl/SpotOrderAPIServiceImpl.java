package com.liujun.trade_ff.core.binance.api.service.spot.impl;

import com.liujun.trade_ff.core.binance.Trade_binance;
import com.liujun.trade_ff.core.binance.api.bean.spot.param.PlaceOrderParam;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.AddOrderResultACK;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.CancelOrderResult;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.QueryOrderResult;
import com.liujun.trade_ff.core.binance.api.client.APIClient;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.enums.NewOrderRespType;
import com.liujun.trade_ff.core.binance.api.service.spot.SpotOrderAPIService;
import com.liujun.trade_ff.core.binance.api.utils.MapUtil;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 币币订单相关接口
 **/
public class SpotOrderAPIServiceImpl implements SpotOrderAPIService {
    private static final Logger log = LoggerFactory.getLogger(SpotOrderAPIServiceImpl.class);
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
        log.info("addOrderACK参数："+ MapUtil.toMapWithNoNullField(param).toString());
        return (AddOrderResultACK)this.client.executeSync(spotOrderAPI.addOrderACK(MapUtil.toMapWithNoNullField(param)));
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

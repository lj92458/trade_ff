package com.liujun.trade_ff.core.binance.api.service.spot;

import com.liujun.trade_ff.core.binance.api.bean.spot.param.PlaceOrderParam;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.AddOrderResultACK;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.CancelOrderResult;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.QueryOrderResult;
import retrofit2.http.Query;

public interface SpotOrderAPIService {

    AddOrderResultACK addOrderACK(PlaceOrderParam param) throws Exception;

    QueryOrderResult queryOrder(String symbol,
                                long orderId,
                                String origClientOrderId,
                                long recvWindow,
                                long timestamp);

    CancelOrderResult cancelOrder(String symbol,
                                  long orderId,
                                  String origClientOrderId,
                                  String newClientOrderId,
                                  long recvWindow,
                                  long timestamp);
}

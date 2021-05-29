package com.liujun.trade_ff.core.binance.api.service.future;

import com.liujun.trade_ff.core.binance.api.bean.futures.param.Order;
import com.liujun.trade_ff.core.binance.api.bean.futures.result.OrderResult;

public interface FutureOrderAPIService {

    OrderResult addOrder(Order order) throws Exception;

    OrderResult queryOrder(String symbol,
                           long orderId,
                           String origClientOrderId,
                           long recvWindow,
                           long timestamp);

    OrderResult cancelOrder(String symbol,
                            long orderId,
                            String origClientOrderId,
                            long recvWindow,
                            long timestamp);
}

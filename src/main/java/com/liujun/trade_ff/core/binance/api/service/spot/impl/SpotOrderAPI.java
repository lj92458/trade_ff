package com.liujun.trade_ff.core.binance.api.service.spot.impl;

import com.liujun.trade_ff.core.binance.api.bean.spot.result.AddOrderResultACK;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.CancelOrderResult;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.QueryOrderResult;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface SpotOrderAPI {

    /**
     * 下单，返回ACK类型的结果
     *
     * @return
     */
    @POST("/api/v3/order")
    @FormUrlEncoded
    @Headers("SECURITY_TYPE:TRADE")
    Call<AddOrderResultACK> addOrderACK(@FieldMap Map<String, Object> map);

    @GET("/api/v3/order")
    @Headers("SECURITY_TYPE:TRADE")
    Call<QueryOrderResult> queryOrder(@Query("symbol") String symbol,
                                      @Query("orderId") long orderId,
                                      @Query("origClientOrderId") String origClientOrderId,
                                      @Query("recvWindow") long recvWindow,
                                      @Query("timestamp") long timestamp);

    @DELETE("/api/v3/order")
    @Headers("SECURITY_TYPE:TRADE")
    Call<CancelOrderResult> cancelOrder(@Query("symbol") String symbol,
                                        @Query("orderId") long orderId,
                                        @Query("origClientOrderId") String origClientOrderId,
                                        @Query("newClientOrderId") String newClientOrderId,
                                        @Query("recvWindow") long recvWindow,
                                        @Query("timestamp") long timestamp);

}

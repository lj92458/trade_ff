package com.liujun.trade_ff.core.binance.api.service.future.impl;

import com.liujun.trade_ff.core.binance.api.bean.futures.result.OrderResult;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

public interface FutureOrderAPI {
    @POST("/dapi/v1/order")
    @FormUrlEncoded
    @Headers("SECURITY_TYPE:TRADE")
    Call<OrderResult> addOrder(@FieldMap Map<String, Object> map);

    @GET("/dapi/v1/order")
    @Headers("SECURITY_TYPE:TRADE")
    Call<OrderResult> queryOrder(@Query("symbol") String symbol,
                                      @Query("orderId") long orderId,
                                      @Query("origClientOrderId") String origClientOrderId,
                                      @Query("recvWindow") long recvWindow,
                                      @Query("timestamp") long timestamp);

    @DELETE("/dapi/v1/order")
    @Headers("SECURITY_TYPE:TRADE")
    Call<OrderResult> cancelOrder(@Query("symbol") String symbol,
                                        @Query("orderId") long orderId,
                                        @Query("origClientOrderId") String origClientOrderId,
                                        @Query("recvWindow") long recvWindow,
                                        @Query("timestamp") long timestamp);

}

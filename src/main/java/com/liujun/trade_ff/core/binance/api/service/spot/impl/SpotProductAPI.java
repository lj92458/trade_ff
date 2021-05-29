package com.liujun.trade_ff.core.binance.api.service.spot.impl;

import com.liujun.trade_ff.core.binance.api.bean.spot.result.Account;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.Depth;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SpotProductAPI {

    /**
     * 查询市场挂单(市场深度)
     * @param symbol 货币交易对
     * @param limit 订单数量
     * @return
     */
    @GET("/api/v3/depth")
    Call<Depth> marketDepth(@Query("symbol") String symbol,
                            @Query("limit") Integer limit);



}

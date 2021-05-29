package com.liujun.trade_ff.core.binance.api.service.future.impl;

import com.liujun.trade_ff.core.binance.api.bean.futures.result.ExchangeInfo;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.Depth;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FutureProductAPI {
    /**
     * 查询市场挂单(市场深度)
     *
     * @param symbol 货币交易对
     * @param limit  订单数量
     * @return
     */
    @GET("/dapi/v1/depth")
    Call<Depth> marketDepth(@Query("symbol") String symbol,
                            @Query("limit") Integer limit);

    /**
     * 获取所有的交易产品,各个合约的详细参数
     *
     * @return
     */
    @GET("/dapi/v1/exchangeInfo")
    Call<ExchangeInfo> getInstruments();

    /**
     * 查询最新的标记价格
     *
     * @param symbol
     * @param interval
     * @param limit
     * @return
     */
    @GET("/dapi/v1/markPriceKlines")
    Call<Object[][]> markPriceKlines(@Query("symbol") String symbol,
                                     @Query("interval") String interval,
                                     @Query("limit") Integer limit);
}

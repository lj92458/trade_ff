package com.liujun.trade_ff.core.binance.api.service.spot.impl;

import com.liujun.trade_ff.core.binance.api.bean.spot.result.Account;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface SpotAccountAPI {

    /**
     * 查询账户信息
     * @param recvWindow 允许的最大延迟 赋值不能大于 60000
     * @param timestamp
     * @return
     */
    @GET("/api/v3/account")
    @Headers("SECURITY_TYPE:USER_DATA")
    Call<Account> accountInfo(@Query("recvWindow") long recvWindow,
                              @Query("timestamp") long timestamp);


}

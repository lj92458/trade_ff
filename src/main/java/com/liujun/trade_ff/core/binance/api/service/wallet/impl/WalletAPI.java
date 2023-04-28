package com.liujun.trade_ff.core.binance.api.service.wallet.impl;

import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

/**
 * Account api
 *
 * @author hucj
 * @version 1.0.0
 * @date 2018/07/04 20:51
 */
public interface WalletAPI {

    @POST("/sapi/v1/capital/withdraw/apply")
    @Headers("SECURITY_TYPE:USER_DATA")
    Call<WithdrawResult> withdraw(@QueryMap Map<String,Object> map);

    @GET("/sapi/v1/capital/config/getall")
    @Headers("SECURITY_TYPE:USER_DATA")
    Call<String> queryAllCoin(@Query("timestamp") long timestamp);
}

package com.liujun.trade_ff.core.binance.api.service.wallet.impl;

import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

import java.util.Map;

/**
 * Account api
 *
 * @author hucj
 * @version 1.0.0
 * @date 2018/07/04 20:51
 */
public interface WalletAPI {

    @POST("/wapi/v3/withdraw.html")
    Call<WithdrawResult> withdraw(@QueryMap Map<String,Object> map);
}

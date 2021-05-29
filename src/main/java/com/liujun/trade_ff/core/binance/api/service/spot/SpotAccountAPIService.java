package com.liujun.trade_ff.core.binance.api.service.spot;

import com.liujun.trade_ff.core.binance.api.bean.spot.result.Account;

/**
 * 币币资产相关接口
 */
public interface SpotAccountAPIService {

    Account accountInfo(long recvWindow);


}

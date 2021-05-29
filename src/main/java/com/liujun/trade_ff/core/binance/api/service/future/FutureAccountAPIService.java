package com.liujun.trade_ff.core.binance.api.service.future;

import com.liujun.trade_ff.core.binance.api.bean.futures.result.Account;

public interface FutureAccountAPIService {

    Account accountInfo(long recvWindow);
}

package com.liujun.trade_ff.core.binance.api.service.wallet;

import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;


public interface WalletAPIService {


    WithdrawResult withdraw(WithdrawParam param) throws Exception;
}

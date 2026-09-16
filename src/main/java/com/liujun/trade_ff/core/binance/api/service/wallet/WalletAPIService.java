package com.liujun.trade_ff.core.binance.api.service.wallet;

import com.liujun.trade_ff.core.binance.api.bean.wallet.param.DepositQueryParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawQueryParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.DepositQueryResult;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawQueryResult;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;


public interface WalletAPIService {


    WithdrawResult withdraw(WithdrawParam param) throws Exception;

    /**
     * 查询提币状态
     *
     * @param param
     * @return
     * @throws Exception
     */
    WithdrawQueryResult withdrawQuery(WithdrawQueryParam param) throws Exception;

    String queryAllCoin(long timestamp);

    /**
     * 查询充值到账情况
     *
     * @param param
     * @return
     * @throws Exception
     */
    DepositQueryResult depositQuery(DepositQueryParam param) throws Exception;

    long queryTime();
}

package com.liujun.trade_ff.core.uniswap.api.rpc;

import com.liujun.trade_ff.core.uniswap.api.bean.Account;
import com.liujun.trade_ff.core.uniswap.api.bean.TransResult;
import hprose.util.concurrent.Promise;

import java.util.List;


public interface AccountRpc {
    //最多5秒返回结果
    Promise<List<Account>> queryTokenBalance(String ethAddress, String[] symbolArr);

    /**
     * 发送代币。如果要发送eth，而我只有weth，就需要把weth变成eth再发送(调用合约的withdraw函数或withdrawTo)
     *
     * @return
     */
    Promise<TransResult> sendToken(String symbol, String address, double amount, boolean needWrap, int maxWaitSeconds, double gasPriceGwei);

    /**
     * 接收别人发来的代币。(本来是不用接收的，但是如果收到eth，需要调用合约的deposit函数转换成weth呢)
     *
     * @return 网络确认数量。0个表示异常
     */
    Promise<Integer> receiveToken(String symbol, String txId, double amount, boolean needWrap, int maxWaitSeconds, double gasPriceGwei);

}

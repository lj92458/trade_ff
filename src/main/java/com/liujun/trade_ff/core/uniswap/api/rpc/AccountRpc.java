package com.liujun.trade_ff.core.uniswap.api.rpc;

import com.liujun.trade_ff.core.uniswap.api.bean.Account;
import hprose.util.concurrent.Promise;

import java.util.List;



public interface AccountRpc {
    //最多5秒返回结果
    Promise<List<Account>> queryTokenBalance(String ethAddress, String[] symbolArr);


}

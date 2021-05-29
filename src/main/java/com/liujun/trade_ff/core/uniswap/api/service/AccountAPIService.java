package com.liujun.trade_ff.core.uniswap.api.service;

import com.liujun.trade_ff.core.uniswap.api.bean.Account;

import java.util.List;

public interface AccountAPIService {
     List<Account> getAccounts(String... symbolArr);


}

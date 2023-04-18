package com.liujun.trade_ff.core.uniswap.api.service.impl;

import com.liujun.trade_ff.core.uniswap.api.bean.APIConfiguration;
import com.liujun.trade_ff.core.uniswap.api.bean.Account;
import com.liujun.trade_ff.core.uniswap.api.rpc.AccountRpc;
import com.liujun.trade_ff.core.uniswap.api.rpc.RpcClient;
import com.liujun.trade_ff.core.uniswap.api.service.AccountAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AccountAPIServiceImpl implements AccountAPIService {
    private static final Logger log = LoggerFactory.getLogger(AccountAPIServiceImpl.class);
    APIConfiguration config;
    AccountRpc accountRpc;

    public AccountAPIServiceImpl(APIConfiguration config) {
        this.config = config;
        this.accountRpc = RpcClient.getInstance(config.getUri()).useService(AccountRpc.class);
    }

    /**
     * 最多25秒返回。
     *
     * @param symbolArr
     * @return
     */
    @Override
    public List<Account> getAccounts(String... symbolArr) {

        int maxRetry = 5;
        for (int retryCount = 0; ; retryCount++) {
            try {

                List<Account> list = accountRpc.queryTokenBalance(config.getAddress(), symbolArr).toFuture().get(10L, TimeUnit.SECONDS);


                return list;
            } catch (Exception e) {
                log.error("queryTokenBalance异常", e);
                if (e.getMessage() != null && e.getMessage().contains("failed to meet quorum")) {
                    log.error("多个节点返回值不一致(继续重试)" + e.getMessage().substring(0, 140));
                    if (retryCount >= maxRetry - 1) {
                        log.error("已经重试了" + maxRetry + "次，不再重试！");
                        throw new RuntimeException(e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e1) {
                        log.error("", e1);
                    }
                } else {
                    throw new RuntimeException(e);
                }
            }

        }//end for
    }
}

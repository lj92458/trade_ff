package com.liujun.trade_ff.core.uniswap.api.service.impl;

import com.liujun.trade_ff.core.uniswap.api.bean.APIConfiguration;
import com.liujun.trade_ff.core.uniswap.api.bean.AddOrderResult;
import com.liujun.trade_ff.core.uniswap.api.rpc.OrderRpc;
import com.liujun.trade_ff.core.uniswap.api.rpc.RpcClient;
import com.liujun.trade_ff.core.uniswap.api.service.OrderAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderApiServiceImpl implements OrderAPIService {
    private static final Logger log = LoggerFactory.getLogger(OrderApiServiceImpl.class);
    APIConfiguration config;
    OrderRpc orderRpc;


    public OrderApiServiceImpl(APIConfiguration config) {
        this.config = config;
        this.orderRpc = RpcClient.getInstance(config.getUri()).useService(OrderRpc.class);
    }

    @Override
    public AddOrderResult addOrder(String coinPair, String orderType, String price, String volume, String gasPriceGwei, double slippage) {

        try {
            log.info("开始调用orderRpc.addOrder");
            //failed to meet quorum 不一定代表失败呢
            AddOrderResult addOrderResult = orderRpc.addOrder(coinPair, orderType, price, volume, config.getMaxWaitSeconds(), gasPriceGwei, slippage).toFuture().get();

            return addOrderResult;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /*
        //
        int maxRetry = 5;
        for (int retryCount = 0; ; retryCount++) {
            try {
                log.info("开始调用orderRpc.addOrder");
                AddOrderResult addOrderResult = orderRpc.addOrder(coinPair, orderType, price, volume, config.getMaxWaitSeconds(), gasPriceGwei,slippage).toFuture().get();

                return addOrderResult;
            } catch (Exception e) {//如果是estimateGas异常，可以重试。如果是下单异常，不能重试。因为可能已经下单成功了
                if (e.getMessage().contains("failed to meet quorum (method=\"estimateGas\"")) {
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
         */
    }

    @Override
    public String queryOrder(String coinPair, String orderId) {
        return "success";
    }

    @Override
    public void cancelOrder(String coinPair, String orderId) {

    }
}

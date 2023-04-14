package com.liujun.trade_ff.core.uniswap.api.service.impl;

import com.liujun.trade_ff.core.uniswap.api.bean.APIConfiguration;
import com.liujun.trade_ff.core.uniswap.api.bean.Book;
import com.liujun.trade_ff.core.uniswap.api.rpc.ProductRpc;
import com.liujun.trade_ff.core.uniswap.api.rpc.RpcClient;
import com.liujun.trade_ff.core.uniswap.api.service.ProductAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ProductAPIServiceImpl implements ProductAPIService {
    private static final Logger log = LoggerFactory.getLogger(ProductAPIServiceImpl.class);
    APIConfiguration config;
    ProductRpc productRpc;

    public ProductAPIServiceImpl(APIConfiguration config) {
        this.config = config;
        this.productRpc = RpcClient.getInstance(config.getUri()).useService(ProductRpc.class);
    }

    /**
     * 查询订单。最多35秒返回。
     * @param coinPair
     * @param marketOrderSize
     * @param orderStepRatio 两个相邻的挂单之间价格差距比例是多少？对少于这个差距的挂单合并。建议为价格的万一/万五。uniswap要求必须大于手续费费率
     * @param poolFee 手续费。 500表示百万分之500，也就是0.0005，也就是0.05%
     * @return
     */
    @Override
    public Book bookProductsByProductId(String coinPair, String marketOrderSize, String orderStepRatio, int poolFee) {

        int maxRetry = 5;
        for (int retryCount = 0; ; retryCount++) {
            try {
                Book book = productRpc.bookProduct(coinPair, marketOrderSize, orderStepRatio, poolFee).toFuture().get(7L, TimeUnit.SECONDS);
                return book;
            } catch (Exception e) {
                if (e.getMessage().contains("failed to meet quorum")) {
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

    /**
     * 查询gas费，以及eth相对某种币的价格。最多25秒返回。
     *
     * @param moneySymbol 交易对中的计价货币
     * @return
     */
    @Override
    public double[] getGasPriceGweiAndEthPrice(String moneySymbol, int poolFee) {

        int maxRetry = 5;
        for (int retryCount = 0; ; retryCount++) {
            try {
                double[] priceArr = productRpc.getGasPriceGweiAndEthPrice(moneySymbol, poolFee).toFuture().get(5L, TimeUnit.SECONDS);
                return priceArr;
            } catch (Exception e) {
                if (e.getMessage().contains("failed to meet quorum")) {
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

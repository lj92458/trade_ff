package com.liujun.trade_ff.core.uniswap.api.rpc;

import com.liujun.trade_ff.core.uniswap.api.bean.Book;
import hprose.util.concurrent.Promise;

public interface ProductRpc {
    //最多需要7秒返回
    Promise<Book> bookProduct(String coinPair, String marketOrderSize, String orderStepRatio, int poolFee);

    /**
     * 查询gas费，以及eth相对某种币的价格
     *
     * @param moneySymbol 交易对中的计价货币
     * @return
     */
    Promise<double[]> getGasPriceGweiAndEthPrice(String moneySymbol,int poolFee);

}

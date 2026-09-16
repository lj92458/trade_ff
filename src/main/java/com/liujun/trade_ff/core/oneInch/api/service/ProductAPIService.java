package com.liujun.trade_ff.core.oneInch.api.service;

import com.liujun.trade_ff.core.uniswap.api.bean.Book;

public interface ProductAPIService {
    /**
     * @param coinPair
     * @return
     */
    Book bookProductsByProductId(String coinPair, double goodsAmount, double moneyAmount);

    /**
     * 查询gas费，以及eth相对某种币的价格
     *
     * @param moneySymbol 交易对中的计价货币
     * @param poolFee     手续费。 500表示百万分之500，也就是0.0005，也就是0.05%
     * @return
     */
    double[] getGasPriceGweiAndEthPrice(String moneySymbol, int poolFee);
}

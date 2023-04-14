package com.liujun.trade_ff.core.uniswap.api.service;

import com.liujun.trade_ff.core.uniswap.api.bean.Book;

public interface ProductAPIService {
     /**
      *
      * @param coinPair
      * @param marketOrderSize
      * @param orderStepRatio 两个相邻的挂单之间价格差距比例是多少？对少于这个差距的挂单合并。建议为价格的万一/万五。uniswap要求必须大于手续费费率
      * @param poolFee 手续费。 500表示百万分之500，也就是0.0005，也就是0.05%
      * @return
      */
     Book bookProductsByProductId(String coinPair, String marketOrderSize, String orderStepRatio,int poolFee);

     /**
      * 查询gas费，以及eth相对某种币的价格
      * @param moneySymbol 交易对中的计价货币
      * @param poolFee 手续费。 500表示百万分之500，也就是0.0005，也就是0.05%
      * @return
      */
     double[] getGasPriceGweiAndEthPrice(String moneySymbol, int poolFee);
}

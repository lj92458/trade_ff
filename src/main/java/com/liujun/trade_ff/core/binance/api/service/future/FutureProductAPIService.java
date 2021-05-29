package com.liujun.trade_ff.core.binance.api.service.future;

import com.liujun.trade_ff.core.binance.api.bean.futures.result.Instrument;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.Depth;

import java.util.List;

public interface FutureProductAPIService {

    Depth marketDepth(String symbol, Integer limit);

    List<Instrument> getInstruments();

    //https://www.binance.com/dapi/v1/markPriceKlines?symbol=BTCUSD_210625&interval=1m&limit=1
    //查询最新的标记价格。平军持仓成本和最新标记价格配合，能知道自己持有的是多单还是空单。
    double lastMarkPrice(String symbol);
}

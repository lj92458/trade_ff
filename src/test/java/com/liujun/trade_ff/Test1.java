package com.liujun.trade_ff;

import com.alibaba.fastjson.JSON;
import com.liujun.trade_ff.core.binance.Trade_binance;
import com.liujun.trade_ff.core.binance.api.bean.common.CoinInfo;
import com.liujun.trade_ff.core.binance.api.bean.common.NetWork;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Test1 {


    private static List<CoinInfo> queryNetWorks(String symbol) throws Exception {
        String json = IOUtils.toString(Test1.class.getClassLoader().getResourceAsStream("other/binance/allCoin.json"), StandardCharsets.UTF_8);
        List<CoinInfo> list = JSON.parseArray(json, CoinInfo.class);
        return list;
    }

    public static void main(String[] args) throws Exception {
        List<CoinInfo> list = queryNetWorks("usdc");
//        System.out.println(list.stream().filter(o->o.getCoin().equalsIgnoreCase("usdc")).toArray().length);
        NetWork netWork= list.stream().filter(o->o.getCoin().equalsIgnoreCase("usdc")).findFirst().get().getNetworkList().get(1);
        System.out.println(netWork.getNetwork());
    }
}

package com.liujun.trade_ff.core;

import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.util.HttpUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 虚拟的交易平台。不存在的
 *
 * @author Administrator
 */
@Component
@Scope("prototype")
public class VirtualTrade extends Trade {

    public static final String platName = "virtual";

    static {

    }

    //---------------------------
    private double feeRate = 0;

    public VirtualTrade(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);

        // 初始查询账户信息。今后只有交易后,才需要重新查询。
        flushAccountInfo();
        setAccInfo(new AccountInfo());
    }

    /**
     * 查询市场深度,填充marketDepth属性。Get
     *
     * @throws Exception
     */
    public void flushMarketDeeps() throws Exception {
        changeMarketPrice(1 - feeRate, 1 + feeRate);
        backupUsefulOrder();
    }

    /**
     * 查询账户资产信息 初始化时,需要查询账户信息。今后只有交易后,才需要重新查询。
     */
    public void flushAccountInfo() throws Exception {

    }

    /**
     * 各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号,然后删除挂单失败的
     */
    public int tradeOrder() throws Exception {

        return 0;
    }

    /**
     * 查出没完全成交的订单
     */
    @Override
    public int queryOrderState() throws Exception {
        return 0;
    }

    /**
     * 撤销没完全成交的订单
     *
     * @throws Exception
     */
    @Override
    public void cancelOrder() throws Exception {

    }

    public String getPlatName() {
        return platName;
    }

    public boolean isActive() {
        return getAccInfo().getFreeGoods() > 0 || getAccInfo().getFreeMoney() > 0;
    }

    /**
     * 提取goods
     *
     * @throws Exception
     */
    @Override
    public void withdraw(String productName, double amount, String address) throws Exception {
        throw new Exception("不支持提币");
    }

}

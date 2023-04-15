package com.liujun.trade_ff.core.thread;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 跨平台套利。现货。快速提交订单，并撤销没有完成的。
 */
@Component
@Scope("prototype")
public class TradeThread extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Engine.class);
    /**
     * 已成交
     */
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");

    // ============ 属性字段
    private Trade trade;
    private Engine engine;
    /**
     * 线程是否正常
     */
    private boolean success = false;
    @Autowired
    Prop prop;


    public TradeThread(Trade trade, Engine engine) {
        this.trade = trade;
        this.engine = engine;
        // 设置线程名称
        setName(trade.getPlatName() + "交易");
    }

    @Override
    public void run() {
        long beginTime = System.currentTimeMillis();
        try {
            // 挂单,并返回挂单数量,
            int orderNum = trade.tradeOrder();
            // 如果挂单数量不为0
            if (orderNum > 0) {
                log_haveTrade.info(trade.getPlatName() + "已挂单" + orderNum + "个：" + trade.getUserOrderList().toString());
                // 查询订单状态，最多4秒
                for (int i = 0; i < 4000 / prop.time_sleep; i++) {
                    TimeUnit.MILLISECONDS.sleep(prop.time_sleep);// 睡眠
                    int unFinishedNum = trade.queryOrderState();
                    if (unFinishedNum == 0) {
                        break;
                    }
                }//end for
                // 撤销没完全成交的订单
                trade.cancelOrder();
                // 并刷新账户信息
                trade.flushAccountInfo();
            } else {
                log.info(trade.getPlatName() + "--------  0 个挂单---------------------------------------");
                // 如果只有0个挂单,说明价格需要倒挂
            }
            success = true;
        } catch (Exception e) {
            log.error(getName() + "异常:" + e.getMessage(), e);

        }
        // 计算耗时
        long endTime = System.currentTimeMillis();
        log.info("线程结束,耗时" + (endTime - beginTime) + "毫秒*********************");
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}

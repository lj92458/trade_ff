package com.liujun.trade_ff.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Created by fengping on 2017/5/14.
 */
@Component
public class Prop {
    @Value("${trade.formatGoodsStr}")
    public String formatGoodsStr;
    @Value("${trade.formatMoneyStr}")
    public String formatMoneyStr;
    @Value("${trade.minCoinNum}")
    public Double minCoinNum;//买卖币时，最小交易金额
    @Value("${trade.moneyPrice}")
    public Double moneyPrice;//计价货币的人民币价格
    public Double huaDian;//滑点，用来强制调平资金。这是一个比例
    public Double huaDian2;//滑点，正常下单时，为了买到。这是一个比例
    //public DecimalFormat fmt_goods;
    //public DecimalFormat fmt_money;
    public String earnWhat;//是钱增加，还是币增加

    @Value("${trade.goods}")
    public String goods;
    @Value("${trade.money}")
    public String money;
    @Value("${engine.time_sleep}")
    public int time_sleep;
    @Value("${trade.orderStepLength}")
    public String orderStepLength;//按价格合并订单，例如：0.1或0.001
    @Value("${trade.marketOrderSize}")
    public int marketOrderSize;// 获取多少个市场挂单？
    @Value("${trade.atLeastEarn}")
    public Double atLeastEarn;//交易一次，最少要赚多少人民币
    @Value("${trade.atLeastRate}")
    public double atLeastRate;//最低利润率(差价除以价格)
    @Value("${trade.earnMoney}")
    public boolean earnMoney;
    @Value("${trade.positionRate}")
    public double positionRate;//仓位上限，占余额的比例。0.5表示50%
    @Value("${log.path}")
    public String logPath;

    public static ThreadLocal<DecimalFormat> fmt_goods;
    public static ThreadLocal<DecimalFormat> fmt_money;

    @PostConstruct
    public void init() {
        huaDian = 5.0 / 100;//滑点，用来强制调平资金.这是一个比例
        huaDian2 = 0.03 / 100;//滑点，正常下单时，为了买到。这是一个比例
        /*
        fmt_goods = new DecimalFormat(this.formatGoodsStr);
        fmt_goods.setRoundingMode(RoundingMode.HALF_UP);

        fmt_money = new DecimalFormat(this.formatMoneyStr);
        fmt_money.setRoundingMode(RoundingMode.HALF_UP);
        */
        fmt_goods = ThreadLocal.withInitial(() -> {
            DecimalFormat fmt = new DecimalFormat(this.formatGoodsStr);
            fmt.setRoundingMode(RoundingMode.HALF_UP);
            return fmt;
        });
        fmt_money = ThreadLocal.withInitial(() -> {
            DecimalFormat fmt = new DecimalFormat(this.formatMoneyStr);
            fmt.setRoundingMode(RoundingMode.HALF_UP);
            return fmt;
        });

        earnWhat = earnMoney ? money : goods;

    }

    public Double formatMoney(Double money) {

        return Double.parseDouble(fmt_money.get().format(money));

    }

    public Double formatGoods(Double goods) {

        return Double.parseDouble(fmt_goods.get().format(goods));

    }
}

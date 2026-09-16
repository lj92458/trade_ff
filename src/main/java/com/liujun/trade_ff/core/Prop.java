package com.liujun.trade_ff.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Format;

/**
 * Created by fengping on 2017/5/14.
 */
@Component
public class Prop {
    @Value("${trade.formatGoodsStr}")
    public String formatGoodsStr;
    @Value("${trade.formatMoneyStr}")
    public String formatMoneyStr;
    @Value("${trade.minTradeValue}")
    public Double minTradeValue;//买卖币时，最小交易金额
    @Value("${trade.moneyPrice}")
    public Double moneyPrice;//计价货币的美元价格
    public Double minMoney;//一次最少要赚的钱
    public Double huaDian;//滑点，用来强制调平资金。这是一个比例
    public Double huaDian2;//滑点，正常下单时，为了买到。这是一个比例
    //public DecimalFormat fmt_goods;
    //public DecimalFormat fmt_money;
    public String earnWhat;//是钱增加，还是币增加

    @Value("${trade.goods}")
    public String goods;
    @Value("${trade.money}")
    public String money;
    /**
     * 每次对平台进行写操作后，休眠多少【毫秒】
     */
    @Value("${engine.time_sleep}")
    public int time_sleep;
    @Value("${trade.marketOrderSize}")
    public int marketOrderSize;// 获取多少个市场挂单？
    @Value("${trade.atLeastEarn}")
    public Double atLeastEarn;//交易一次，最少要赚多少美元
    @Value("${trade.atLeastRate}")
    public double atLeastRate;//最低利润率(差价除以价格)
    @Value("${trade.earnMoney}")
    public boolean earnMoney;
    @Value("${trade.positionRate}")
    public double positionRate;//仓位上限，占余额的比例。0.5表示50%
    @Value("${logging.file.path}")
    public String logPath;

    public double minTradeMoney;

    public static ThreadLocal<DecimalFormat> fmt_goods;
    public static ThreadLocal<DecimalFormat> fmt_money;
    /**
     * 一个非常小的值，接近于零
     */
    public String transTokenFormatStr = "0.000000";//minAmount小数位数，必须跟transTokenFormatStr位数保持一致，否则engine.balanceToken的while循环是死循环
    //0.000003是个非常小的数，假设币价10万美元，0.000003才价值0.3美元，面对2000美元的以太币，它才价值0.006美元
    public double minAmount = 3.0 / (Double.parseDouble(transTokenFormatStr.replace("0.", "1")));
    public DecimalFormat transTokenFromat = new DecimalFormat(transTokenFormatStr);

    @PostConstruct
    public void init() {
        transTokenFromat.setRoundingMode(RoundingMode.DOWN);
        minTradeMoney = 10.0 / moneyPrice;
        minMoney = atLeastEarn / this.moneyPrice;//一次最少要赚的钱
        //滑点，用来强制调平资金.这是一个比例. 1%应该够了吧？如果平台深度不足以吃到足够的单，再把这个滑点调大。
        //但是调大了有个副作用：矫枉过正。例如让把多余的goods卖出去，结果多卖的比例就等于这个滑点
        huaDian = 1.0 / 100;
        huaDian2 = 0.03 / 100;//滑点，正常下单时，为了买到。这是一个比例
        /*
        fmt_goods = new DecimalFormat(this.formatGoodsStr);
        fmt_goods.setRoundingMode(RoundingMode.DOWN);

        fmt_money = new DecimalFormat(this.formatMoneyStr);
        fmt_money.setRoundingMode(RoundingMode.DOWN);
        */
        fmt_goods = ThreadLocal.withInitial(() -> {
            DecimalFormat fmt = new DecimalFormat(this.formatGoodsStr);
            fmt.setRoundingMode(RoundingMode.DOWN);
            return fmt;
        });
        fmt_money = ThreadLocal.withInitial(() -> {
            DecimalFormat fmt = new DecimalFormat(this.formatMoneyStr);
            fmt.setRoundingMode(RoundingMode.DOWN);
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

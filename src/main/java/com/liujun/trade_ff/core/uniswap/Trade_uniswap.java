package com.liujun.trade_ff.core.uniswap;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;

import com.liujun.trade_ff.core.uniswap.api.service.WalletAPIService;
import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawParam;
import com.liujun.trade_ff.core.uniswap.api.bean.WithdrawResult;
import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketDepth;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.uniswap.api.bean.APIConfiguration;
import com.liujun.trade_ff.core.uniswap.api.bean.Account;
import com.liujun.trade_ff.core.uniswap.api.bean.AddOrderResult;
import com.liujun.trade_ff.core.uniswap.api.bean.Book;
import com.liujun.trade_ff.core.uniswap.api.service.AccountAPIService;
import com.liujun.trade_ff.core.uniswap.api.service.OrderAPIService;
import com.liujun.trade_ff.core.uniswap.api.service.ProductAPIService;
import com.liujun.trade_ff.core.uniswap.api.service.impl.AccountAPIServiceImpl;
import com.liujun.trade_ff.core.uniswap.api.service.impl.OrderApiServiceImpl;
import com.liujun.trade_ff.core.uniswap.api.service.impl.ProductAPIServiceImpl;
import com.liujun.trade_ff.core.uniswap.api.service.impl.WalletAPIServiceImpl;
import com.liujun.trade_ff.core.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
public class Trade_uniswap extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_uniswap.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
    public static final String platName = "uniswap";


    // ===============================
    private APIConfiguration config;
    private ProductAPIService productAPIService;
    private AccountAPIService accountAPIService;
    private OrderAPIService orderAPIService;
    private WalletAPIService walletAPIService;
    /**
     * 网址前缀
     */
    @Value("${uniswap.url}")
    private String url_prex;

    @Value("${uniswap.ethAddress}")
    private String ethAddress;
    @Value("${time_oneCycle}")
    private int maxWaitSeconds;
    /**
     * 批量下单的最大批量
     */
    private int max_batch_amount_trad = 10;
    @Value("${uniswap.gasPercent}")
    private double gasPercent;

    @Value("${uniswap.feeRate}")
    private double feeRate;
    private String coinPair;
    private double gasPriceGwei;
    //------------------------


    public Trade_uniswap(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);


    }

    @PostConstruct
    private void init() {
        this.config = new APIConfiguration();
        config.setUri(url_prex);
        config.setAddress(ethAddress);
        config.setMaxWaitSeconds(80);

        this.productAPIService = new ProductAPIServiceImpl(this.config);
        this.orderAPIService = new OrderApiServiceImpl(this.config);
        this.accountAPIService = new AccountAPIServiceImpl(this.config);
        this.walletAPIService = new WalletAPIServiceImpl(this.config);
        coinPair = prop.goods + "-" + prop.money;
        try {
            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();

        } catch (Exception e) {

            log.error(getPlatName() + " : " + e.getMessage(), e);
        }

        initSuccess = true;
    }

    /**
     * 查询市场深度,填充marketDepth属性。Get
     *
     * @throws Exception aa
     */
    public void flushMarketDeeps() throws Exception {
        // 初始化,清空
        MarketDepth depth = getMarketDepth();
        depth.getAskList().clear();
        depth.getBidList().clear();
        try {
            Book book = productAPIService.bookProductsByProductId(coinPair, prop.marketOrderSize + "", "" + prop.orderStepLength);

            // 卖方挂单
            List<String[]> askArr = book.getAsks();
            for (String[] value : askArr) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(Double.parseDouble(value[0]));
                marketOrder.setVolume(Double.parseDouble(value[1]));
                marketOrder.setPlatId(platId);

                depth.getAskList().add(marketOrder);
            }
            // 买方挂单
            List<String[]> bidArr = book.getBids();
            for (String[] strings : bidArr) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(Double.parseDouble(strings[0]));
                marketOrder.setVolume(Double.parseDouble(strings[1]));
                marketOrder.setPlatId(platId);

                depth.getBidList().add(marketOrder);
            }

            sort(depth);// 排序
            changeMarketPrice(1 - feeRate, 1 + feeRate);
            backupUsefulOrder();
            // 设置当前价格
            double askPrice = depth.getAskList().get(0).getPrice();
            double bidPrice = depth.getBidList().get(0).getPrice();
            setCurrentPrice((bidPrice + askPrice) / 2.0);
            //
        } catch (Exception e) {
            // log.error(getPlatName()+"" + e.getMessage());
            throw e;
        }
    }

    /**
     * 查询账户资产信息 .Post 初始化时,需要查询账户信息。今后只有交易后,才需要重新查询。
     */
    public void flushAccountInfo() throws Exception {
        try {
            AccountInfo accountInfo = new AccountInfo();
            List<Account> list = accountAPIService.getAccounts(prop.goods, prop.money);
            for (Account acc : list) {
                if (acc.getCurrency().equalsIgnoreCase(prop.goods)) {
                    accountInfo.setFreeGoods(Double.parseDouble(acc.getAvailable()));
                    accountInfo.setFreezedGoods(Double.parseDouble(acc.getHold()));
                }
                if (acc.getCurrency().equalsIgnoreCase(prop.money)) {
                    accountInfo.setFreeMoney(Double.parseDouble(acc.getAvailable()));
                    accountInfo.setFreezedMoney(Double.parseDouble(acc.getHold()));
                }
            }
            //

            super.setAccInfo(accountInfo);
            //查询gas费，然后设置矿工费
            double[] priceArr;
            //如果goods是eth，就直接采用当前市场价
            if (prop.goods.equals("eth") && getCurrentPrice() != 0) {
                double gasPrice = productAPIService.getGasPriceGweiAndEthPrice("eth")[0];
                double ethPrice = getCurrentPrice();
                priceArr = new double[]{gasPrice, ethPrice};
            } else {
                priceArr = productAPIService.getGasPriceGweiAndEthPrice(prop.money);
            }
            this.gasPriceGwei = adjustGasPrice(priceArr[0]);
            double limit = 0;
            /*
            approve  44,339
            removeLiquidityETHWithPermit   178198  189810     172680
            addLiquidity 157,282
            addLiquidityETH 2706648
            swapExactTokensForTokens 192253    156245  184603     178443    178614  130113
            swapExactTokensForTokensSupportingFeeOnTransferTokens 232,635
            swapETHForExactTokens 132,772
            swapExactETHForTokens 117,096    111404    135285
            swapExactTokensForETH 110,876    113087   127844  100526 110590  139288~151304
            swapTokensForExactETH



             */
            if (prop.goods.equals("eth") || prop.money.equals("eth")) {//swapExactETHForTokens或swapExactTokensForETH
                limit = 155000;
            } else {//swapExactTokensForTokens
                limit = 200000;
            }
            double feeInEth = limit * this.gasPriceGwei / 1_000_000_000;//假设需要gas14万个，那么总共需要的eth是多少？
            //把eth价值，转化成本交易对中的money
            double feeInMoney;
            feeInMoney = feeInEth * priceArr[1];
            log.info("gas价格：" + this.gasPriceGwei + "Gwei,矿工费:" + feeInMoney + prop.money + "(" + prop.formatMoney(feeInMoney * prop.moneyPrice) + "人民币)");
            super.setFixFee(feeInMoney);

        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }

    }

    private double adjustGasPrice(double gasPrice) {
        double percent;
        /*
        //乐观型策略
        if (gasPrice < 10) {
            percent = 100 / 100.0;
        } else if (gasPrice < 95) {
            percent = 88 / 100.0;
        } else {
            percent = 83 / 100.0;
        }
        */
        /*
        //中性策略(不乐观不悲观)
        if (gasPrice < 10) {
            percent = 100 / 100.0;
        } else if (gasPrice < 95) {
            percent = 93 / 100.0;
        } else {
            percent = 88 / 100.0;
        }
         */
        /*
        //悲观型策略
        if (gasPrice < 10) {
            percent = 100 / 100.0;
        } else if (gasPrice < 95) {
            percent = 95 / 100.0;
        } else {
            percent = 91 / 100.0;
        }
        */

        percent = gasPercent;
        return Double.parseDouble(new DecimalFormat("0.0000").format(gasPrice * percent));

    }

    /**
     * 各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号,然后删除挂单失败的
     */
    public int tradeOrder() throws Exception {
        log.info(getPlatName() + "开始下单");
        List<UserOrder> userOrderList = getUserOrderList();

        int orderCount = 0;// 有效订单的数量
        // 删掉无效订单
        for (int i = userOrderList.size() - 1; i >= 0; i--) {
            if (!userOrderList.get(i).isEnable()) {
                userOrderList.remove(i);// 无效订单要及时删掉，否则help_tradeOneBatch里面定位错误。但是不能在预处理时删。
            }
        }// end for

        merge();//对订单进行合并
        changeMyOrderPrice(1 - feeRate, 1 + feeRate);
        for (; orderCount < userOrderList.size(); orderCount++) {
            UserOrder order = userOrderList.get(orderCount);

            // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
            double addPrice = (order.getType().equals("sell") ? -1 * prop.huaDian2 : prop.huaDian2);

            AddOrderResult result = this.orderAPIService.addOrder(coinPair, order.getType(),
                    (order.getPrice() * (1 + addPrice)) + "",
                    order.getVolume() + "",
                    this.gasPriceGwei + "",
                    super.profitRate);
            // 设置orderId

            order.setOrderId("" + result.getOrderId());


        }// end for


        return userOrderList.size();
    }


    /**
     * 查出完全成交的订单，并且标记。那么，没被标记的，就是不成功的
     */
    @Override
    public int queryOrderState() throws Exception {
        List<UserOrder> userOrderList = getUserOrderList();

        // 查询完全成交的
        for (UserOrder o : userOrderList) {
            String status = orderAPIService.queryOrder(coinPair, o.getOrderId());
            if (status.equals("success")) {
                o.setFinished(true);
            }
        }

        int unFinishedNum = 0;//统计没成交的订单
        for (int i = userOrderList.size() - 1; i >= 0; i--) {
            UserOrder order = userOrderList.get(i);
            if (!order.isFinished()) {
                unFinishedNum++;
            }
        }// end for
        return unFinishedNum;
    }

    /**
     * 撤销没完全成交的订单
     *
     * @throws Exception
     */
    @Override
    public void cancelOrder() throws Exception {

        List<UserOrder> userOrderList = getUserOrderList();
        List<UserOrder> finishedList = new ArrayList<>();
        double haveEarn = 0;// 至少赚了这么多
        // 删掉完全成交的
        for (int i = userOrderList.size() - 1; i >= 0; i--) {
            UserOrder order = userOrderList.get(i);
            log.debug(getPlatName() + "当前订单是否完成?：" + order.isFinished() + ",订单内容:" + order);
            if (order.isFinished()) {
                userOrderList.remove(i);
                finishedList.add(order);
                haveEarn += order.getDiffPrice() * order.getVolume();
            }
        }// end for
        log.info("-----uniswap已删掉" + finishedList.size() + "个已成交的,还剩" + userOrderList.size() + "个未成交");
        log_haveTrade.info("uniswap++++++++++++++至少赚了" + prop.formatMoney(haveEarn) + ". 完全成交" + finishedList.size() + "个订单：" + finishedList.toString());

        // userOrderList里面剩下的是没完全成交的,全部撤单。一次最多撤10个

        for (UserOrder o : userOrderList) {
            orderAPIService.cancelOrder(coinPair, o.getOrderId());


        }

    }

    public String getPlatName() {
        return platName;
    }

    /**
     * 提取资产
     *
     * @throws Exception
     */
    @Override
    public void withdraw(String productName, double amount, String address) throws Exception {
        WithdrawParam param = new WithdrawParam();
        param.setAsset(productName);
        param.setAddress(address);
        param.setAmount(amount);


        WithdrawResult result = this.walletAPIService.withdraw(param);
        if (result.isSuccess()) {
            log.info("提币成功：" + result.getId() + ":" + result.getMsg());
        } else {
            log.error("提币失败：" + result.getMsg());
            throw new Exception("提币失败：" + result.getMsg());
        }
    }
}

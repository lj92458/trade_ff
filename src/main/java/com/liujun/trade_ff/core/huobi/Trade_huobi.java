package com.liujun.trade_ff.core.huobi;

import com.huobi.client.RequestOptions;
import com.huobi.client.SyncRequestClient;
import com.huobi.client.model.*;
import com.huobi.client.model.enums.AccountType;
import com.huobi.client.model.enums.BalanceType;
import com.huobi.client.model.enums.OrderState;
import com.huobi.client.model.enums.OrderType;
import com.huobi.client.model.request.NewOrderRequest;
import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketDepth;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
public class Trade_huobi extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_huobi.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");

    public static final String platName = "huobi";

    // ===============================
    private SyncRequestClient syncRequestClient;

    /**
     * 网址前缀
     */
    @Value("${huobi.url}")
    private String url_prex;

    @Value("${huobi.apiKey}")
    private String apiKey;
    @Value("${huobi.secretKey}")
    private String secretKey;
    @Value("${huobi.feeRate}")
    private double feeRate;
    private String coinPair;//交易对
    /**
     * 批量下单的最大批量
     */
    private int max_batch_amount_trad = 10;
    //==================  ======================


    public Trade_huobi(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);

    }

    @PostConstruct
    private void init() {
        RequestOptions options = new RequestOptions();
        options.setUrl(url_prex);
        syncRequestClient = SyncRequestClient.create(apiKey, secretKey, options);

        String money2 = prop.money.endsWith("btc") ? "btc" : prop.money;
        coinPair = prop.goods + money2;
        try {
            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();
        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
        }


        this.initSuccess = true;
    }

    /**
     * 查询市场深度,填充marketDepth属性。Get
     *
     * @throws Exception
     */
    public void flushMarketDeeps() throws Exception {
        // 初始化,清空
        MarketDepth depth = getMarketDepth();
        depth.getAskList().clear();
        depth.getBidList().clear();
        try {
            PriceDepth priceDepth = syncRequestClient.getPriceDepth(coinPair, prop.marketOrderSize);
            List<DepthEntry> askList = priceDepth.getAsks();
            // 卖方挂单
            for (int i = 0; i < askList.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(askList.get(i).getPrice().doubleValue());
                marketOrder.setVolume(askList.get(i).getAmount().doubleValue());
                marketOrder.setPlatId(platId);

                depth.getAskList().add(marketOrder);
            }
            // 买方挂单
            List<DepthEntry> bidList = priceDepth.getBids();
            for (int i = 0; i < bidList.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(bidList.get(i).getPrice().doubleValue());
                marketOrder.setVolume(bidList.get(i).getAmount().doubleValue());
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

        } catch (Exception e) {
            // log.error(getPlatName()+"" + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询账户资产信息。 初始化时,需要查询账户信息。今后只有交易后,才需要重新查询。
     */
    public void flushAccountInfo() throws Exception {


        AccountInfo accountInfo = new AccountInfo();

        Account acc = syncRequestClient.getAccountBalance(AccountType.SPOT);
        List<Balance> moneyBalanceList = acc.getBalance(prop.money);
        for (Balance bal : moneyBalanceList) {
            if (bal.getType().equals(BalanceType.TRADE)) {
                accountInfo.setFreeMoney(bal.getBalance().doubleValue());
            }
            if (bal.getType().equals(BalanceType.FROZEN)) {
                accountInfo.setFreezedMoney(bal.getBalance().doubleValue());
            }
        }
        List<Balance> goodsBalanceList = acc.getBalance(prop.goods);
        for (Balance bal : goodsBalanceList) {
            if (bal.getType().equals(BalanceType.TRADE)) {
                accountInfo.setFreeGoods(bal.getBalance().doubleValue());
            }
            if (bal.getType().equals(BalanceType.FROZEN)) {
                accountInfo.setFreezedGoods(bal.getBalance().doubleValue());
            }
        }

        setAccInfo(accountInfo);


    }

    /**
     * 各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号,然后删除挂单失败的
     */
    public int tradeOrder() throws Exception {
        log.info(getPlatName() + "开始下单");
        List<UserOrder> userOrderList = getUserOrderList();
        List<NewOrderRequest> batch = null;
        List<List<NewOrderRequest>> batchList = new ArrayList<>();
        int orderCount = 0;// 有效订单的数量
        // 删掉无效订单
        for (int i = userOrderList.size() - 1; i >= 0; i--) {
            if (!userOrderList.get(i).isEnable()) {
                userOrderList.remove(i);// 无效订单要及时删掉
            }
        }// end for
        merge();//对订单进行合并
        changeMyOrderPrice(1 - feeRate, 1 + feeRate);
        for (; orderCount < userOrderList.size(); orderCount++) {
            UserOrder order = userOrderList.get(orderCount);

            // 如果新的批次开始,就结束前面批次
            if (0 == orderCount % max_batch_amount_trad) {
                if (orderCount != 0) {// 如果前面有批次
                    batchList.add(batch);
                }
                batch = new ArrayList<>();
            }

            // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
            double addPrice = (order.getType().equals("sell") ? -1 * prop.huaDian2 : prop.huaDian2);
            NewOrderRequest request = new NewOrderRequest(coinPair, AccountType.SPOT,
                    order.getType().equals("buy") ? OrderType.BUY_LIMIT : OrderType.SELL_LIMIT,
                    new BigDecimal(order.getVolume() + 0),
                    new BigDecimal(order.getPrice() * (1 + addPrice)));


            batch.add(request);

        }// end for

        // for循环完毕后,最后一个批次肯定还没了结。
        if (0 != orderCount) {// 如果有订单
            batchList.add(batch);
            log.info(getPlatName() + "待挂单有" + batchList.size() + "个批次:" + batchList.toString());
        }

        // 对每个批次的orders_data进行挂单
        for (int i = 0; i < batchList.size(); i++) {

            log.info(getPlatName() + "当前是第" + i + "批：" + batchList.get(i));
            List<CreateOrderResult> resultList = syncRequestClient.batchCreateOrder(batchList.get(i));

            for (int j = 0; j < resultList.size(); j++) {
                // 当前订单在userOrderList中的下标
                int orderIndex = i * max_batch_amount_trad + j;
                CreateOrderResult result = resultList.get(j);

                // 设置orderId
                UserOrder thisOrder = userOrderList.get(orderIndex);
                if (thisOrder == null) {
                    throw new Exception(getPlatName() + "获取本地订单异常：null,index:" + orderIndex + ",size:" + userOrderList.size());
                }
                log.info(getPlatName() + " ???????????本批次第" + j + "个订单是" + thisOrder + "吗? 总index" + orderIndex);
                thisOrder.setOrderId("" + result.getOrderId());
                if (result.getErrCode() != null) {
                    log.error(getPlatName() + "下单失败：" + result.getErrMsg());
                }
            }// end for
        }// outter for
        return userOrderList.size();
    }

    /**
     * 查出没完全成交的订单
     */
    @Override
    public int queryOrderState() throws Exception {
        List<UserOrder> userOrderList = getUserOrderList();

        for (UserOrder userOrder : userOrderList) {

            Order o = syncRequestClient.getOrder(coinPair, Long.parseLong(userOrder.getOrderId()));
            if (o.getState().equals(OrderState.FILLED)) {
                userOrder.setFinished(true);
            } else {
                userOrder.setFinished(false);
                log.warn(getPlatName() + ":订单没完全成交" + o.getState() + ".订单" + userOrder);
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
        List<UserOrder> finishedList = new ArrayList<UserOrder>();
        double haveEarn = 0;// 至少赚了这么多
        // 删掉完全成交的
        for (int i = userOrderList.size() - 1; i >= 0; i--) {
            UserOrder order = userOrderList.get(i);
            if (order.isFinished()) {
                userOrderList.remove(i);
                finishedList.add(order);
                haveEarn += order.getDiffPrice() * order.getVolume();
            }
        }// end for
        log_haveTrade.info(getPlatName() + "++++++++++++++至少赚了" + prop.formatMoney(haveEarn) + ". 完全成交" + finishedList.size() + "个订单：" + finishedList.toString());

        // userOrderList里面剩下的是没完全成交的,全部撤单
        List<Long> orderIdList = new ArrayList<>();
        for (UserOrder order : userOrderList) {
            orderIdList.add(Long.parseLong(order.getOrderId()));


        }// end for
        if (orderIdList.size() > 0) {
            syncRequestClient.cancelOrders(coinPair, orderIdList);
        }
    }

    public String getPlatName() {
        return platName;
    }

    /**
     * 提取btc
     *
     * @throws Exception
     */
    @Override
    public void withdraw(String productName, double amount, String address) throws Exception {
        throw new Exception("不支持提币");
    }


}

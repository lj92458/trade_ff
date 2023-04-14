package com.liujun.trade_ff.core.okcoin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketDepth;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import com.okcoin.commons.okex.open.api.bean.account.param.Withdraw;
import com.okcoin.commons.okex.open.api.bean.account.result.WithdrawFee;
import com.okcoin.commons.okex.open.api.bean.spot.param.OrderParamDto;
import com.okcoin.commons.okex.open.api.bean.spot.param.PlaceOrderParam;
import com.okcoin.commons.okex.open.api.bean.spot.result.Account;
import com.okcoin.commons.okex.open.api.bean.spot.result.Book;
import com.okcoin.commons.okex.open.api.bean.spot.result.OrderInfo;
import com.okcoin.commons.okex.open.api.bean.spot.result.OrderResult;
import com.okcoin.commons.okex.open.api.config.APIConfiguration;
import com.okcoin.commons.okex.open.api.enums.I18nEnum;
import com.okcoin.commons.okex.open.api.service.account.AccountAPIService;
import com.okcoin.commons.okex.open.api.service.account.impl.AccountAPIServiceImpl;
import com.okcoin.commons.okex.open.api.service.spot.SpotAccountAPIService;
import com.okcoin.commons.okex.open.api.service.spot.SpotOrderAPIServive;
import com.okcoin.commons.okex.open.api.service.spot.SpotProductAPIService;
import com.okcoin.commons.okex.open.api.service.spot.impl.SpotAccountAPIServiceImpl;
import com.okcoin.commons.okex.open.api.service.spot.impl.SpotOrderApiServiceImpl;
import com.okcoin.commons.okex.open.api.service.spot.impl.SpotProductAPIServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 第五版api https://github.com/CollmeYH/okex-java-sdk-api-v5
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
public class Trade_okcoin extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_okcoin.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
    public static final String platName = "okcoin";


    // ===============================
    private APIConfiguration config;
    private SpotProductAPIService spotProductAPIService;
    private SpotAccountAPIService spotAccountAPIService;
    private SpotOrderAPIServive spotOrderAPIServive;
    private AccountAPIService accountAPIService;
    /**
     * 网址前缀
     */
    @Value("${okcoin.url}")
    private String url_prex;
    @Value("${okcoin.passphrase}")
    private String passphrase;//解密密码
    /**
     * 批量下单的最大批量
     */
    private int max_batch_amount_trad = 10;


    @Value("${okcoin.apiKey}")
    private String apiKey;
    @Value("${okcoin.secretKey}")
    private String secretKey;
    @Value("${okcoin.feeRate}")
    private double feeRate;
    @Value("${okcoin.goods}")
    private String goods;
    @Value("${okcoin.money}")
    private String money;
    private String coinPair;
    //------------------------


    public Trade_okcoin(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);


    }

    @PostConstruct
    private void init() {
        this.config = new APIConfiguration();
        config.setEndpoint(url_prex);
        config.setApiKey(apiKey);
        config.setSecretKey(secretKey);
        config.setPassphrase(passphrase);

        config.setPrint(false);
        config.setI18n(I18nEnum.SIMPLIFIED_CHINESE);
        this.spotProductAPIService = new SpotProductAPIServiceImpl(this.config);
        this.spotAccountAPIService = new SpotAccountAPIServiceImpl(this.config);
        this.spotOrderAPIServive = new SpotOrderApiServiceImpl(this.config);
        this.accountAPIService = new AccountAPIServiceImpl(this.config);
        coinPair = goods.toUpperCase() + "-" + money.toUpperCase();
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
     * @throws Exception
     */
    public void flushMarketDeeps() throws Exception {
        // 初始化,清空
        MarketDepth depth = getMarketDepth();
        depth.getAskList().clear();
        depth.getBidList().clear();
        try {
            Book book = spotProductAPIService.bookProductsByProductId(coinPair, prop.marketOrderSize + "", "" + prop.orderStepLength);

            // 卖方挂单
            List<String[]> askArr = book.getAsks();
            for (int i = 0; i < askArr.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(Double.parseDouble(askArr.get(i)[0]));
                marketOrder.setVolume(Double.parseDouble(askArr.get(i)[1]));
                marketOrder.setPlatId(platId);

                depth.getAskList().add(marketOrder);
            }
            // 买方挂单
            List<String[]> bidArr = book.getBids();
            for (int i = 0; i < bidArr.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(Double.parseDouble(bidArr.get(i)[0]));
                marketOrder.setVolume(Double.parseDouble(bidArr.get(i)[1]));
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
            List<Account> list = spotAccountAPIService.getAccounts();
            for (Account acc : list) {
                if (acc.getCurrency().equalsIgnoreCase(goods)) {
                    accountInfo.setFreeGoods(Double.parseDouble(acc.getAvailable()));
                    accountInfo.setFreezedGoods(Double.parseDouble(acc.getHold()));
                }
                if (acc.getCurrency().equalsIgnoreCase(money)) {
                    accountInfo.setFreeMoney(Double.parseDouble(acc.getAvailable()));
                    accountInfo.setFreezedMoney(Double.parseDouble(acc.getHold()));
                }
            }
            //
            setAccInfo(accountInfo);

        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }

    }

    /**
     * 各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号,然后删除挂单失败的
     */
    public int tradeOrder() throws Exception {
        log.info(getPlatName() + "开始下单");
        // 构造多个批次(List<PlaceOrderParam>)
        List<UserOrder> userOrderList = getUserOrderList();
        List<PlaceOrderParam> batch = null;
        List<List<PlaceOrderParam>> batchList = new ArrayList<>();
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

            // 如果新的批次开始,就结束前面批次
            if (0 == orderCount % max_batch_amount_trad) {
                if (orderCount != 0) {// 如果前面有批次
                    batchList.add(batch);
                }
                batch = new ArrayList<>();
            }

            // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
            double addPrice = (order.getType().equals("sell") ? -1 * prop.huaDian2 : prop.huaDian2);

            PlaceOrderParam orderParam = new PlaceOrderParam();
            orderParam.setInstrument_id(coinPair);
            orderParam.setPrice(Double.toString(order.getPrice() * (1 + addPrice)));
            orderParam.setType("market");// market limit
            orderParam.setSide(order.getType());
            orderParam.setSize(Double.toString(order.getVolume() - 0.00));
            orderParam.setOrder_type("0");
            batch.add(orderParam);

        }// end for

        // for循环完毕后,最后一个批次肯定还没了结。
        if (0 != orderCount) {// 如果有订单
            batchList.add(batch);
            log.info(getPlatName() + "待挂单有" + batchList.size() + "个批次:" + batchList.toString());
        }

        // 对每个批次的orders_data进行挂单
        for (int i = 0; i < batchList.size(); i++) {

            log.info(getPlatName() + "当前是第" + i + "批：" + batchList.get(i));
            Map<String, List<OrderResult>> orderResult = this.spotOrderAPIServive.addOrders(batchList.get(i));


            // 结果数组
            List<OrderResult> resultList = orderResult.get(goods + "-" + money);

            if (resultList == null) {
                log.warn(JSON.toJSONString(orderResult));
            }
            for (int j = 0; j < resultList.size(); j++) {
                // 当前订单在userOrderList中的下标
                int orderIndex = i * max_batch_amount_trad + j;
                OrderResult result = resultList.get(j);

                // 设置orderId
                UserOrder thisOrder = userOrderList.get(orderIndex);
                if (thisOrder == null) {
                    throw new Exception(getPlatName() + "获取本地订单异常：null,index:" + orderIndex + ",size:" + userOrderList.size());
                }
                log.info(getPlatName() + " ???????????本批次第" + j + "个订单是" + thisOrder + "吗? 总index" + orderIndex);
                thisOrder.setOrderId("" + result.getOrder_id());
                if (!result.isResult()) {
                    log.error(getPlatName() + "下单失败：" + result.getError_message());
                }
            }// end for
        }// outter for


        return userOrderList.size();
    }


    /**
     * 查出完全成交的订单，并且标记。那么，没被标记的，就是不成功的
     */
    @Override
    public int queryOrderState() throws Exception {
        List<UserOrder> userOrderList = getUserOrderList();
        //找到最大订单号的后面那个订单号，作为after参数。防止查出更新的订单
        Long after = Long.parseLong(userOrderList.get(userOrderList.size() - 1).getOrderId()) + 1;
        //找到最小订单号的前面那个订单号，作为before参数。防止查出早期的旧订单
        Long before = Long.parseLong(userOrderList.get(0).getOrderId()) - 1;
        // 查询完全成交的
        List<OrderInfo> infoList = spotOrderAPIServive.getOrders(coinPair, "2", "" + after, "" + before, "");
        for (OrderInfo info : infoList) {

            String orderId = info.getOrder_id();
            UserOrder order = findOrderById(orderId);
            if (order != null) {
                order.setFinished(true);
                log.info(getPlatName() + "完全成交:" + info.toString());
            }
        }// end for


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
        log.info("-----okcoin已删掉" + finishedList.size() + "个已成交的,还剩" + userOrderList.size() + "个未成交");
        log_haveTrade.info("okcion++++++++++++++至少赚了" + prop.formatMoney(haveEarn) + ". 完全成交" + finishedList.size() + "个订单：" + finishedList.toString());

        // userOrderList里面剩下的是没完全成交的,全部撤单。一次最多撤10个
        List<OrderParamDto> cancleOrders = new ArrayList<>();
        OrderParamDto dto = new OrderParamDto();
        dto.setInstrument_id(coinPair);
        List<String> order_ids = new ArrayList<>();
        if (userOrderList.size() > 0) {
            for (UserOrder userOrder : userOrderList) {
                order_ids.add(userOrder.getOrderId());
            }
            dto.setOrder_ids(order_ids);
            cancleOrders.add(dto);

            this.spotOrderAPIServive.batchCancleOrders_2(cancleOrders);
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
        List<WithdrawFee> feeResult = this.accountAPIService.getWithdrawFee(productName);

        Withdraw withdraw = new Withdraw();
        withdraw.setTo_address(address);
        withdraw.setFee(feeResult.get(0).getMin_fee().toString());
        withdraw.setCurrency(productName);
        withdraw.setAmount("" + amount);
        withdraw.setDestination("4");
        withdraw.setTrade_pwd("");
        JSONObject drawResult = this.accountAPIService.withdraw(withdraw);
        log.info("提币：" + drawResult);
    }
}

package com.liujun.trade_ff.core.binance;

import com.alibaba.fastjson2.JSON;
import com.binance.connector.client.WebSocketStreamClient;
import com.binance.connector.client.impl.WebSocketStreamClientImpl;
import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.binance.api.bean.common.CoinInfo;
import com.liujun.trade_ff.core.binance.api.bean.common.NetWork;
import com.liujun.trade_ff.core.binance.api.bean.spot.param.PlaceOrderParam;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.*;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.DepositQueryParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawQueryParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.DepositQueryResult;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawQueryResult;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.enums.*;
import com.liujun.trade_ff.core.binance.api.service.spot.SpotAccountAPIService;
import com.liujun.trade_ff.core.binance.api.service.spot.SpotOrderAPIService;
import com.liujun.trade_ff.core.binance.api.service.spot.SpotProductAPIService;
import com.liujun.trade_ff.core.binance.api.service.spot.impl.SpotAccountAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.service.spot.impl.SpotOrderAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.service.spot.impl.SpotProductAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.service.wallet.WalletAPIService;
import com.liujun.trade_ff.core.binance.api.service.wallet.impl.WalletAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;
import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.modle.WebSocketState;
import com.liujun.trade_ff.core.util.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

 /*

    API接口文档(包含杠杆）：https://binance-docs.github.io/apidocs/spot/cn/#general-info
期货接口文档：https://binance-docs.github.io/apidocs/futures/cn/#185368440e
API报错自查链接：https://github.com/binance-exchange/binance-official-api-docs/blob/f92d9df35cd926a3514618666ca6ca494c1a734d/errors_CN.md
API交易规则说明：https://binance.zendesk.com/hc/zh-cn/articles/115003235691
API常见问题 (FAQ)：https://binance.zendesk.com/hc/zh-cn/articles/360004492232
查看交易平台各项限定，例如对提交订单的限定  https://www.binance.com/api/v3/exchangeInfo
     */

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
public class Trade_binance extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_binance.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
    public static final String platName = "binance";


    // ===============================
    private APIConfiguration config;
    private SpotProductAPIService spotProductAPIService;
    private SpotAccountAPIService spotAccountAPIService;
    private SpotOrderAPIService spotOrderAPIService;
    private WalletAPIService walletAPIService;
    /**
     * 网址前缀
     */
    @Value("${binance.url}")
    private String url_prex;

    /**
     * 批量下单的最大批量
     */
    private int max_batch_amount_trad = 10;
    /**
     * 信息传递的最大延迟（毫秒）
     */
    private long recvWindow = 5000L;


    @Value("${binance.apiKey}")
    private String apiKey;
    @Value("${binance.secretKey}")
    private String secretKey;
    @Value("${binance.feeRate}")
    private double feeRate;
    @Value("${binance.websocket.url}")
    private String websocketUrl;
    @Value("${binance.websocket.level}")
    private int websocketLevel;
    @Value("${binance.websocket.speed}")
    private int websocketSpeed;
    private String coinPair;
    private List<CoinInfo> coinInfoList;
    private WebSocketStreamClient webSocketStreamClient;
    private Depth depth;
    private long timeAdd = -1000;//本机时间与币安时间的差距: 用币安时间减去本机时间
    //------------------------


    public Trade_binance(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);


    }

    @PostConstruct
    private void init() throws IOException {
        this.config = new APIConfiguration();
        config.setEndpoint(url_prex);
        config.setApiKey(apiKey);
        config.setSecretKey(secretKey);
        config.setPrint(false);
        config.setI18n(I18nEnum.SIMPLIFIED_CHINESE);
        this.spotProductAPIService = new SpotProductAPIServiceImpl(this.config);
        this.spotAccountAPIService = new SpotAccountAPIServiceImpl(this.config);
        this.spotOrderAPIService = new SpotOrderAPIServiceImpl(this.config);
        this.walletAPIService = new WalletAPIServiceImpl(this.config);
        coinPair = token[0] + token[1];
        try {
            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();
            //flushMarketDeeps();
        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);

        }
        //String json = IOUtils.toString(Trade_binance.class.getResourceAsStream("allCoin.json"), StandardCharsets.UTF_8);
        String json = walletAPIService.queryAllCoin(getBinanceTime());
        coinInfoList = JSON.parseArray(json, CoinInfo.class);

        //连接websocket
        webSocketStreamClient = new WebSocketStreamClientImpl(websocketUrl);
        WebSocketState webSocketState = new WebSocketState();
        int depthConnectionId = webSocketStreamClient.partialDepthStream(coinPair, websocketLevel, websocketSpeed, dataStr -> {
            try {
                if (engine.stop) {
                    cleanResource();
                }
                //每30秒查询币安系统时间
                if ((engine.i * engine.time_queryOrder) % 30 == 0) {
                    timeAdd = walletAPIService.queryTime() - DateUtils.getUnixTimeMilli();
                    log.warn("本机系统时间落后" + timeAdd);
                }
                webSocketState.setLastUpdateTime(System.currentTimeMillis());
                depth = JSON.parseObject(dataStr, Depth.class);
                webSocketState.setLastUpdateId(depth.getLastUpdateId());
                new Thread(() -> {
                    try {
                        this.createMarketDepth();
                        engine.processMarketDepth(this);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, "wss_" + platName).start();
            } catch (Exception e) {
                log.error("", e);
            }
        });
        webSocketState.setConnectionId(depthConnectionId);
        webSocketStateMap.put(WebSocketState.StreamType.depth, webSocketState);

        //end
        this.initSuccess = true;
    }

    public void cleanResource() {
        if (webSocketStreamClient != null) {
            webSocketStreamClient.closeConnection(webSocketStateMap.get(WebSocketState.StreamType.depth).getConnectionId());
        }
    }

    /**
     * 查询市场深度,填充marketDepth属性。Get
     *
     * @throws Exception
     */
    public void flushMarketDeeps() throws Exception {
        //depth = spotProductAPIService.marketDepth(coinPair, prop.marketOrderSize);
        //如果marketOrderList数据被取走了，就等待websocket主动填充
        synchronized (getMarketDepth()) {
            while (getMarketDepth()[0].isEmpty()) {
                getMarketDepth().wait();
            }
        }

    }

    private void createMarketDepth() throws Exception {
        synchronized (getMarketDepth()) {
            ArrayList<MarketOrder>[] marketOrderList = getMarketDepth();
            try {
                List<String[]>[] listArr = new List[]{depth.getAsks(), depth.getBids()};
                for (int i = 0; i < 2; i++) {
                    marketOrderList[i].clear();
                    for (String[] strings : listArr[i])
                        marketOrderList[i].add(new MarketOrder(platId, Double.parseDouble(strings[0]), Double.parseDouble(strings[1])));
                }

                sort(marketOrderList);// 排序
                changeMarketPrice(1 - feeRate, 1 + feeRate);
                //backupUsefulOrder();//这行代码放到了engine.queryMarketDepth，因为wss线程调用flushMarketDeeps时不应该改动backupDepth
                // 设置当前价格
                setCurrentPrice((marketOrderList[0].get(0).getPrice() + marketOrderList[1].get(0).getPrice()) / 2.0);
            } catch (Exception e) {
                log.error(getPlatName() + e.getMessage());
                marketOrderList[0].clear();
                marketOrderList[1].clear();
                throw e;
            } finally {
                getMarketDepth().notifyAll();
            }
        }
    }


    /**
     * 查询账户资产信息 .Post 初始化时,需要查询账户信息。今后只有交易后,才需要重新查询。
     */
    public void flushAccountInfo() throws Exception {
        try {
            AccountInfo accountInfo = new AccountInfo();
            Account account = spotAccountAPIService.accountInfo(recvWindow, getBinanceTime());
            if (!account.getAccountType().equalsIgnoreCase(SymbolType.SPOT.toString())) {
                throw new Exception("当前账户不是spot账户");
            }
            for (Balance bal : account.getBalance()) {
                for (int i = 0; i < 2; i++) {
                    if (bal.getAsset().equalsIgnoreCase(token[i])) {
                        accountInfo.freeToken[i] = Double.parseDouble(bal.getFree());
                        accountInfo.freezedToken[i] = Double.parseDouble(bal.getLocked());
                        accountInfo.totalToken[i] = accountInfo.freeToken[i] + accountInfo.freezedToken[i];
                    }
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
     * 注意过滤器是否开启。如果开启，可能导致小额交易失败 https://binance-docs.github.io/apidocs/spot/cn/#cc81fff589
     * 2022-06-15，币安添加新的过滤器 NOTIONAL(名义价值过滤器)，基于minNotional 与 maxNotional 值来限制名义价值 (price * quantity).
     * 每个交易对都有不同的要求。要求【"symbol": "CELOBUSD"】交易额在一个区间内10~9000000美元：{  信息来源:交易规范信息 https://www.binance.com/api/v3/exchangeInfo
     * "filterType": "NOTIONAL",
     * "minNotional": "10.00000000",
     * "applyMinToMarket": true,
     * "maxNotional": "9000000.00000000",
     * "applyMaxToMarket": false,
     * "avgPriceMins": 5
     * }
     * }
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
        for (; orderCount < userOrderList.size(); orderCount++) {
            UserOrder order = userOrderList.get(orderCount);

            //如果需要调拨资金，就不要提交订单
            if (tokenTransferDex2Cex(order))
                return 0;

            changeMyOrderPrice(1 - feeRate, 1 + feeRate);//这一行一定要在tokenTransferDex2Cex之后，否则，传过去的币不够用
            // 为了确保能成交，可以根据滑点调整价格。
            double addPrice = (order.getType().equals("sell") ? -1 * prop.huaDian2 : prop.huaDian2);
            PlaceOrderParam param = new PlaceOrderParam(recvWindow, timeAdd);
            param.setSymbol(coinPair);//symbol
            param.setSide(Enum.valueOf(OrderSide.class, order.getType().toUpperCase()));// orderSide
            param.setType(OrderType.LIMIT);//todo LIMIT还是MARKET
            if (param.getType().equals(OrderType.LIMIT) || param.getType().equals(OrderType.STOP_LOSS_LIMIT) || param.getType().equals(OrderType.TAKE_PROFIT_LIMIT)) {
                param.setTimeInForce(TimeInForce.GTC);//timeInForce
                param.setPrice(Double.parseDouble(Prop.fmt_money.get().format(order.getPrice() * (1 + addPrice))));// price
            }
            if (order.getType().equalsIgnoreCase("buy")) {
                //市价单可以用setQuoteOrderQty替代setQuantity
                //param.setQuoteOrderQty(Double.parseDouble(Prop.fmt_money.get().format(Math.min(accInfo.freeToken[1], order.getPrice() * order.getVolume()))));
                param.setQuantity(Double.parseDouble(Prop.fmt_goods.get().format(Math.min(accInfo.freeToken[1] / param.getPrice(), order.getVolume()))));// quantity
            } else {//sell
                param.setQuantity(Double.parseDouble(Prop.fmt_goods.get().format(Math.min(accInfo.freeToken[0], order.getVolume()))));
            }
            try {
                this.onTrading = true;
                AddOrderResultACK result = this.spotOrderAPIService.addOrderACK(param);
                // 设置orderId
                order.setOrderId("" + result.getOrderId());
            } finally {
                this.onTrading = false;
            }


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
            QueryOrderResult result = spotOrderAPIService.queryOrder(coinPair, Long.parseLong(o.getOrderId()),
                    null, recvWindow, getBinanceTime());
            if (result.getStatus().equals(OrderStatus.FILLED.toString())) {
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
        log.info("-----binance已删掉" + finishedList.size() + "个已成交的,还剩" + userOrderList.size() + "个未成交");
        if (haveEarn > 0) {
            log_haveTrade.info("binance++++++++++++++至少赚了" + prop.formatMoney(haveEarn) + ". 完全成交" + finishedList.size() + "个订单：" + finishedList.toString());
        }
        // userOrderList里面剩下的是没完全成交的,全部撤单。一次最多撤10个

        for (UserOrder o : userOrderList) {
            CancelOrderResult result = this.spotOrderAPIService.cancelOrder(coinPair, Long.parseLong(o.getOrderId()),
                    null, null, recvWindow, getBinanceTime());


        }


    }

    public String getPlatName() {
        return platName;
    }

    /**
     * 提取资产。为什么一定要等待，直到被打包呢？因为要拿到哈希值。有了哈希值，才能调用nodeJS的receiveToken服务，进而把eth包装成weth
     *
     * @param needWrap 只有dex需要
     * @return Trade.WithdrawResult txId为""表示异常. 链上交易哈希
     * @throws Exception
     */
    @Override
    public Trade.WithdrawResult withdraw(String productName, double amount, String address, String netWorkShort, boolean needWrap) throws Exception {
        String myOrderId = System.currentTimeMillis() + "";
        WithdrawParam param = new WithdrawParam(productName, address, amount, recvWindow, timeAdd);
        //提取货物，就用货物的网络
        List<NetWork> netWorks = coinInfoList.stream().filter(o -> o.getCoin().equalsIgnoreCase(productName)).findFirst().get().getNetworkList();
        String netWork = netWorks.stream().filter(o -> o.getNetwork().toUpperCase().contains(netWorkShort.toUpperCase()) || o.getName().toUpperCase().contains(netWorkShort.toUpperCase())).findFirst().get().getNetwork();
        param.setNetwork(netWork);
        param.setWithdrawOrderId(myOrderId);
        param.setTransactionFeeFlag(true);// 手续费从谁扣
        com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult result = this.walletAPIService.withdraw(param);
        log.info(getPlatName() + "提币请求已提交，id=" + result.getId() + "，请求参数" + param);
        if (result.getId() == null) {
            return null;
        }
        //轮番查询状态，直到返回链上交易哈希tranId. 最多等10分钟
        int sleepSecond = 3;
        for (int i = 0; i < 10 * 60 / sleepSecond; i++) {
            Thread.sleep(1000 * sleepSecond);//等3秒
            try {
                WithdrawQueryParam queryParam = new WithdrawQueryParam(recvWindow, timeAdd);
                queryParam.setWithdrawOrderId(myOrderId);
                WithdrawQueryResult queryResult = this.walletAPIService.withdrawQuery(queryParam);
                if (queryResult == null) {
                    log.info("等待binance提现到账，queryResult=null");
                } else if (queryResult.getStatus() == 6) {
                    log.info("binance提币已到账" + ", ConfirmNo=" + queryResult.getConfirmNo());
                    return new Trade.WithdrawResult(queryResult.getTxId(), Double.parseDouble(queryResult.getAmount()));
                } else {
                    log.info("等待binance提现到账，status=" + queryResult.getStatus() + ", ConfirmNo=" + queryResult.getConfirmNo() + ", info=" + queryResult.getInfo());
                }
            } catch (Exception e) {
                log.error("walletAPIService.withdrawQuery异常：", e);
            }
        }//end for
        return null;
    }

    /**
     * @param asset
     * @param txId
     * @param amount
     * @param needWrap
     * @return 收到资金量。-1表示异常
     */
    @Override
    public double depositToken(String asset, String txId, double amount, boolean needWrap) throws Exception {
        //轮番查询状态，直到返回链上交易哈希tranId. 最多等10分钟
        int sleepSecond = 3;//每三秒查询一次
        for (int i = 0; i < 10 * 60 / sleepSecond; i++) {
            Thread.sleep(1000 * sleepSecond);
            try {
                DepositQueryParam param = new DepositQueryParam(recvWindow, timeAdd);
                param.setTxId(txId);
                DepositQueryResult queryResult = this.walletAPIService.depositQuery(param);
                if (queryResult != null && queryResult.getStatus() == 6) {//1表示成功，但需要25分钟才能成功。因此用6表示“以上账但不能提取”，只要账户有其它币用来锁定，那么本次充的币就可以提走
                    log.info("binance充值已到账" + ", 确认次数confirmTimes=" + queryResult.getConfirmTimes());
                    return Double.parseDouble(queryResult.getAmount());
                } else {
                    if (queryResult != null) {
                        log.info("等待binance充值到账，status=" + queryResult.getStatus() + ", 确认次数confirmTimes=" + queryResult.getConfirmTimes());
                    } else {
                        log.info("等待binance充值到账，queryResult=null");
                    }
                }
            } catch (Exception e) {
                log.error("walletAPIService.depositQuery异常：", e);
            }
        }//end for
        return -1;
    }

    private long getBinanceTime() {
        return DateUtils.getUnixTimeMilli() + timeAdd;
    }

    @Value("${binance.goods}")
    public void setGoods(String goods) {
        token[0] = goods.toUpperCase();
    }

    @Value("${binance.money}")
    public void setMoney(String money) {
        token[1] = money.toUpperCase();
    }

    @Value("${binance.goodsAddress}")
    public void setGoodsAddress(String goodsAddress) {
        tokenAddress[0] = goodsAddress;
    }

    @Value("${binance.moneyAddress}")
    public void setMoneyAddress(String moneyAddress) {
        tokenAddress[1] = moneyAddress;
    }

    @Value("${binance.goodsNetWork}")
    public void setGoodsNetWork(String goodsNetWork) {
        tokenNetWork[0] = StringUtils.isEmpty(goodsNetWork) ? new String[0] : goodsNetWork.split(",");
    }

    @Value("${binance.moneyNetWork}")
    public void setMoneyNetWork(String moneyNetWork) {
        tokenNetWork[1] = StringUtils.isEmpty(moneyNetWork) ? new String[0] : moneyNetWork.split(",");
    }
}

package com.liujun.trade_ff.core.binance;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.binance.api.bean.spot.param.PlaceOrderParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawParam;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.*;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;
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
import java.util.ArrayList;
import java.util.List;

 /*

    API接口文档(包含杠杆）：https://binance-docs.github.io/apidocs/spot/en/#general-info
期货接口文档：https://binance-docs.github.io/apidocs/futures/cn/#185368440e
API报错自查链接：https://github.com/binance-exchange/binance-official-api-docs/blob/f92d9df35cd926a3514618666ca6ca494c1a734d/errors_CN.md
API交易规则说明：https://binance.zendesk.com/hc/zh-cn/articles/115003235691
API常见问题 (FAQ)：https://binance.zendesk.com/hc/zh-cn/articles/360004492232
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
    private long recvWindow = 5000;


    @Value("${binance.apiKey}")
    private String apiKey;
    @Value("${binance.secretKey}")
    private String secretKey;
    @Value("${binance.feeRate}")
    private double feeRate;
    private String coinPair;
    //------------------------


    public Trade_binance(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);


    }

    @PostConstruct
    private void init() {
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
        String money2 = prop.money.endsWith("btc") ? "btc" : prop.money;
        coinPair = prop.goods.toUpperCase() + money2.toUpperCase();
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

            Depth depthResult = spotProductAPIService.marketDepth(coinPair, prop.marketOrderSize);
            // 卖方挂单
            List<String[]> askArr = depthResult.getAsks();
            for (int i = 0; i < askArr.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(Double.parseDouble(askArr.get(i)[0]));
                marketOrder.setVolume(Double.parseDouble(askArr.get(i)[1]));
                marketOrder.setPlatId(platId);

                depth.getAskList().add(marketOrder);
            }
            // 买方挂单
            List<String[]> bidArr = depthResult.getBids();
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
            Account account = spotAccountAPIService.accountInfo(recvWindow);
            if (!account.getAccountType().equalsIgnoreCase(SymbolType.SPOT.toString())) {
                throw new Exception("当前账户不是spot账户");
            }

            for (Balance bal : account.getBalance()) {
                if (bal.getAsset().equalsIgnoreCase(prop.goods)) {
                    accountInfo.setFreeGoods(Double.parseDouble(bal.getFree()));
                    accountInfo.setFreezedGoods(Double.parseDouble(bal.getLocked()));
                }
                if (bal.getAsset().equalsIgnoreCase(prop.money)) {
                    accountInfo.setFreeMoney(Double.parseDouble(bal.getFree()));
                    accountInfo.setFreezedMoney(Double.parseDouble(bal.getLocked()));
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
            PlaceOrderParam param = new PlaceOrderParam();
            param.setSymbol(coinPair);//symbol
            param.setSide(Enum.valueOf(OrderSide.class, order.getType().toUpperCase()));// orderSide
            param.setType(OrderType.LIMIT);// orderType
            param.setTimestamp(DateUtils.getUnixTimeMilli());//
            param.setTimeInForce(TimeInForce.GTC);//timeInForce
            param.setQuantity(order.getVolume() - 0.00);// quantity
            param.setPrice(order.getPrice() * (1 + addPrice));// price
            param.setRecvWindow(recvWindow);// recvWindow

            AddOrderResultACK result = this.spotOrderAPIService.addOrderACK(param);
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
            QueryOrderResult result = spotOrderAPIService.queryOrder(coinPair, Long.parseLong(o.getOrderId()),
                    null, recvWindow, DateUtils.getUnixTimeMilli());
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
        log_haveTrade.info("binance++++++++++++++至少赚了" + prop.formatMoney(haveEarn) + ". 完全成交" + finishedList.size() + "个订单：" + finishedList.toString());

        // userOrderList里面剩下的是没完全成交的,全部撤单。一次最多撤10个

        for (UserOrder o : userOrderList) {
            CancelOrderResult result = this.spotOrderAPIService.cancelOrder(coinPair, Long.parseLong(o.getOrderId()),
                    null, null, recvWindow, DateUtils.getUnixTimeMilli());


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
        param.setRecvWindow(recvWindow);
        param.setTimestamp(DateUtils.getUnixTimeMilli());

        WithdrawResult result = this.walletAPIService.withdraw(param);
        if (result.isSuccess()) {
            log.info("提币成功：" + result.getId() + ":" + result.getMsg());
        } else {
            log.error("提币失败：" + result.getMsg());
            throw new Exception("提币失败：" + result.getMsg());
        }

    }


}

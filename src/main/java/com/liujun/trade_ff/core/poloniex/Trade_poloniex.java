package com.liujun.trade_ff.core.poloniex;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;

import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketDepth;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by fengping on 2017/5/15.
 */
@Component
@Scope("prototype")
public class Trade_poloniex extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_poloniex.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
    public static final String platName = "poloniex";
    public static final String priceType = "BTC_LTC";
    /**
     * 市场深度.使用时，需要拼接参数：&depth=10
     */
    public static final String url_depth = "https://poloniex.com/public?command=returnOrderBook&currencyPair=" + priceType;
    /**
     * 交易接口rul
     */
    public static final String url_prex = "https://poloniex.com/tradingApi";
    /**
     * 用户资产信息查询
     */
    private static final String url_userInfo = "";
    /**
     * 挂单
     */
    private static final String url_trade = "";
    /**
     * 批量查询订单信息
     */
    private static final String url_qurey = "";
    /**
     * 撤单
     */
    private static final String url_cancelOrder = "";

    private static final double feeRate = 0.0025;

    @Value("${poloniex.apiKey}")
    private String apiKey;
    @Value("${poloniex.secretKey}")
    private String secretKey;
    //--------------------------

    /**
     * 市场买单系数
     */
    private final double buyRate = (1 - feeRate) * usdRate;
    /**
     * 市场卖单系数
     */
    private final double sellRate = (1 + feeRate) * usdRate;
    PoloniexTradingAPIClient poloClient;
    //-----------------------------

    public Trade_poloniex(HttpUtil httpUtil, int platId, double usdRate,Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate,prop, engine);
        try {
            if (usdRate == 0) {
                throw new Exception("汇率不能为0");
            }
            poloClient = new PoloniexTradingAPIClient(apiKey, secretKey);

            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();
        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }
    }


    public static String getNextSeq2() {
        long val = System.currentTimeMillis() / 100L - 14330625078L;
        return val + "";
    }

    /**
     * 查询市场深度,填充marketDepth属性。Get
     *
     * @throws Exception
     */
    @Override
    public void flushMarketDeeps() throws Exception {
        // 初始化,清空
        MarketDepth depth = getMarketDepth();
        depth.getAskList().clear();
        depth.getBidList().clear();
        try {
            String jsonStr = httpUtil.requestHttpGet("", url_depth, "depth=" + prop.marketOrderSize);
            JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            JSONArray askArr = jsonObject.getJSONArray("asks");
            JSONArray bidArr = jsonObject.getJSONArray("bids");
            // 卖方挂单
            for (int i = 0; i < askArr.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(askArr.getJSONArray(i).getDouble(0));
                marketOrder.setVolume(askArr.getJSONArray(i).getDouble(1));
                marketOrder.setPlatId(platId);

                depth.getAskList().add(marketOrder);
            }
            // 买方挂单
            for (int i = 0; i < bidArr.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(bidArr.getJSONArray(i).getDouble(0));
                marketOrder.setVolume(bidArr.getJSONArray(i).getDouble(1));
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
     * 查询账户资产信息 初始化时,需要查询账户信息。今后只有交易后,才需要重新查询。
     */
    @Override
    public void flushAccountInfo() throws Exception {
        try {
            //String nonce = getNextSeq2();

            String jsonStr = poloClient.returnCompleteBalances();

            //
            log.debug("jsonStr" + jsonStr);
            JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            if (jsonObject.getString("error") != null) {
                throw new Exception("返回错误消息:" + jsonObject.getString("error"));
            }
            AccountInfo accountInfo = new AccountInfo();
            double freeMoney = jsonObject.getJSONObject("BTC").getDouble("available");
            double freeGoods = jsonObject.getJSONObject("LTC").getDouble("available");
            double freezedCNY = jsonObject.getJSONObject("BTC").getDouble("onOrders");
            double freezedGoods = jsonObject.getJSONObject("LTC").getDouble("onOrders");
            accountInfo.setFreeMoney(freeMoney * usdRate);
            accountInfo.setFreeGoods(freeGoods);
            accountInfo.setFreezedMoney(freezedCNY * usdRate);
            accountInfo.setFreezedGoods(freezedGoods);
            setAccInfo(accountInfo);

        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号,然后删除挂单失败的
     */
    @Override
    public int tradeOrder() throws Exception {
        log.info(getPlatName() + "开始下单");
        List<UserOrder> userOrderList = getUserOrderList();
        // 删掉无效订单
        for (int i = userOrderList.size() - 1; i >= 0; i--) {
            if (!userOrderList.get(i).isEnable()) {
                userOrderList.remove(i);// 无效订单要及时删掉
            }
        }// end for
        merge();//对订单进行合并
        changeMyOrderPrice(1 - feeRate, 1 + feeRate);
        for (int orderCount = 0; orderCount < userOrderList.size(); orderCount++) {
            UserOrder order = userOrderList.get(orderCount);
            try {
                //String nonce = getNextSeq2();
                //生成header
                double addPrice = (order.getType().equals("buy") ? prop.huaDian2 : -1 * prop.huaDian2);
                //immediateOrCancel参数，让系统自动取消没完全成交的
                /*String params = "&immediateOrCancel=1&currencyPair=" + priceType +  "&rate=" + (order.getPrice() + addPrice) + "&amount=" + (order.getVolume() - 0.01);
                String postData = "command=" + order.getType() + params + "&nonce=" + nonce;
                Map<String, String> headerMap = new HashMap<String, String>();
                headerMap.put("Key", apiKey);
                headerMap.put("Sign", encryUtil.sign(postData));
                String jsonStr = HttpUtil2.httpPost(url_prex + url_trade, headerMap, postData);
                */
                String jsonStr = null;
                if (order.getType().equals("buy")) {
                    jsonStr = poloClient.buy(priceType, new BigDecimal(order.getPrice()*( 1+ addPrice)), new BigDecimal(order.getVolume() - 0.01), false, true, false);
                } else {
                    jsonStr = poloClient.sell(priceType, new BigDecimal(order.getPrice()*(1 + addPrice)), new BigDecimal(order.getVolume() - 0.01), false, true, false);
                }
                // 返回的对象
                JSONObject jsonObject = JSONObject.fromObject(jsonStr);
                if (jsonObject.getString("error") != null) {//如果失败
                    order.disableOrder();
                    order.setOrderId("-1");
                    log.warn(getPlatName() + "挂单失败:" + "返回错误消息:" + jsonObject.getString("error"));
                } else {
                    // 设置orderId
                    order.setOrderId(jsonObject.getString("orderNumber"));
                    log.info(getPlatName() + "挂单成功:" + order + ",jsonStr:" + jsonStr);
                }

            } catch (Exception e) {
                // log.error(e.getMessage(), e);
                throw e;
            }


        }// end for


        // 删除挂单失败的、无效的、完全成交的
        for (int i = userOrderList.size() - 1; i >= 0; i--) {
            UserOrder order = userOrderList.get(i);
            if (!order.isEnable() || order.getOrderId().equals("-1")) {
                userOrderList.remove(i);
            }
        }
        return userOrderList.size();
    }

    /**
     * 查出没完全成交的订单。
     */
    @Override
    public int queryOrderState() throws Exception {
        return 0;//无需查询。系统会自动取消没成交的
    }

    /**
     * 取消订单
     *
     * @throws Exception
     */
    @Override
    public void cancelOrder() throws Exception {
        //无需执行。系统会自动取消没成交的
    }

    @Override
    public String getPlatName() {
        return platName;
    }

    @Override
    public void withdraw(String productName, double amount, String address) throws Exception {
        throw new Exception("不支持提币");
    }
}

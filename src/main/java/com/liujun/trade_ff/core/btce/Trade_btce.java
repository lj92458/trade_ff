package com.liujun.trade_ff.core.btce;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;

import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketDepth;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import com.liujun.trade_ff.core.util.HttpUtil2;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class Trade_btce extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_btce.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");

    public static final String platName = "btce";
    public static final String priceType = "ltc_btc";
    // ===============================
    /**
     * 网址前缀
     */
    private static final String url_prex = "https://btc-e.com/tapi";//
    /**
     * 市场深度
     */
    private static final String url_depth = "https://btc-e.com/api/3/depth/" + priceType;
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
    /** 批量下单的最大批量 */
    // private static final int max_batch_amount_trad = 5;
    /** 批量查询订单的最大批量 */
    //private static final int max_batch_amount_queryOrder = 100;
    /**
     * 交易手续费费率
     */
    private static final double feeRate = 0.002;

    @Value("${btce.apiKey}")
    private String apiKey;
    @Value("${btce.secretKey}")
    private String secretKey;
    //=============  对象属性==================

    private EncryDigestUtil encryUtil;
    /**
     * 市场买单系数
     */
    private final double buyRate = (1 - feeRate) * usdRate;
    /**
     * 市场卖单系数
     */
    private final double sellRate = (1 + feeRate) * usdRate;

    //=========================================
    static {

    }

    public Trade_btce(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);
        try {
            if (usdRate == 0) {
                throw new Exception("汇率不能为0");
            }

            encryUtil = new EncryDigestUtil(apiKey, secretKey);

            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();
        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取一个自增长的整数。不用，有更好的策略
     *
     * @throws IOException
     */
    public static String getNextSeq() throws Exception {
        BufferedReader in = null;
        BufferedWriter out = null;
        try {
            in = new BufferedReader(new FileReader("sequence_btce.data"));
            long value = Integer.parseInt(in.readLine());
            if (value > 4294967294l) {
                throw new Exception("序列已达到最大值");
            }
            in.close();
            out = new BufferedWriter(new FileWriter("sequence_btce.data"));
            out.write(++value + "");
            out.close();
            log.info("btce自增长序列:" + value);
            return value + "";
        } finally {
        }

    }

    public String getNextSeq2() {
        long val = System.currentTimeMillis() / 100L - 14330625078L;
        return val + "";
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
            String jsonStr = httpUtil.requestHttpGet("", url_depth, "limit=" + prop.marketOrderSize);
            JSONObject jsonObject = JSONObject.fromObject(jsonStr).getJSONObject(priceType);
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
    public void flushAccountInfo() throws Exception {
        try {
            String nonce = getNextSeq2();
            //生成header
            String postData = "method=getInfo" + "&nonce=" + nonce;
            log.debug("postData:" + postData);
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put("Key", apiKey);
            String signStr = encryUtil.sign(postData);
            log.debug("签名" + signStr);
            headerMap.put("Sign", signStr);
            String jsonStr = HttpUtil2.httpPost(url_prex + url_userInfo, headerMap, postData);
            log.debug("jsonStr" + jsonStr);
            JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            if (jsonObject.getInt("success") == 0) {
                throw new Exception("返回错误消息:" + jsonObject.getString("error"));
            }
            AccountInfo accountInfo = new AccountInfo();
            double freeMoney = jsonObject.getJSONObject("return").getJSONObject("funds").getDouble("usd");
            double freeGoods = jsonObject.getJSONObject("return").getJSONObject("funds").getDouble("btc");
            double freezedMoney = 0;
            double freezedGoods = 0;
            accountInfo.setFreeMoney(freeMoney * usdRate);
            accountInfo.setFreeGoods(freeGoods);
            accountInfo.setFreezedMoney(freezedMoney * usdRate);
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
                String nonce = getNextSeq2();
                //生成header
                double addPrice = (order.getType().equals("buy") ? prop.huaDian2 : -1 * prop.huaDian2);
                String params = "&pair=" + priceType + "&type=" + order.getType() + "&rate=" + (order.getPrice() * (1 + addPrice)) + "&amount=" + (order.getVolume() - 0.01);
                String postData = "method=Trade" + params + "&nonce=" + nonce;
                Map<String, String> headerMap = new HashMap<String, String>();
                headerMap.put("Key", apiKey);
                headerMap.put("Sign", encryUtil.sign(postData));
                String jsonStr = HttpUtil2.httpPost(url_prex + url_trade, headerMap, postData);
                // 返回的对象
                JSONObject jsonObject = JSONObject.fromObject(jsonStr);
                if (jsonObject.getInt("success") == 0) {//如果失败
                    order.disableOrder();
                    order.setOrderId("-1");
                    log.warn(getPlatName() + "挂单失败:" + "返回错误消息:" + jsonObject.getString("error"));
                } else {
                    // 设置orderId
                    order.setOrderId(jsonObject.getJSONObject("return").getInt("order_id") + "");
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
     * 查出没完全成交的订单
     */
    @Override
    public int queryOrderState() throws Exception {
        List<UserOrder> userOrderList = getUserOrderList();
        // 先将订单默认设为“已完全成交”
        for (UserOrder o : userOrderList) {//
            o.setFinished(true);
        }
        // 查询,
        for (UserOrder o : userOrderList) {
            if (o.getOrderId().equals("0")) {//如果已完成，就不用查询
                continue;
            }
            String nonce = getNextSeq2();
            //生成header
            String params = "&order_id=" + o.getOrderId();
            String postData = "method=OrderInfo" + params + "&nonce=" + nonce;
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put("Key", apiKey);
            headerMap.put("Sign", encryUtil.sign(postData));
            String jsonStr = HttpUtil2.httpPost(url_prex + url_qurey, headerMap, postData);
            // 返回的对象
            JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            if (jsonObject.getInt("success") == 0) {//如果失败
                log.warn(getPlatName() + "订单查询失败:" + "返回错误消息:" + jsonObject.getString("error"));
            } else {
                JSONObject jsonObj = jsonObject.getJSONObject("return").getJSONObject(o.getOrderId());
                if (jsonObj != null) {
                    if (jsonObj.getInt("status") == 0) {
                        o.setFinished(false);
                    }
                } else {
                    log.warn(getPlatName() + "订单没有找到:" + jsonStr);
                }
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
        for (UserOrder order : userOrderList) {
            String nonce = getNextSeq2();
            //生成header
            String params = "&order_id=" + order.getOrderId();
            String postData = "method=CancelOrder" + params + "&nonce=" + nonce;
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put("Key", apiKey);
            headerMap.put("Sign", encryUtil.sign(postData));
            String jsonStr = HttpUtil2.httpPost(url_prex + url_cancelOrder, headerMap, postData);
            // 返回的对象
            //JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            String msg = getPlatName() + "撤单返回消息：" + jsonStr;
            log.info(msg);
            //

        }// end for

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

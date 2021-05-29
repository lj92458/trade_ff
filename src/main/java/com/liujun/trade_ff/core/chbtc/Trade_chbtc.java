package com.liujun.trade_ff.core.chbtc;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
@Component
@Scope("prototype")
public class Trade_chbtc extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_chbtc.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");

    public static final String platName = "chbtc";
    // 错误码
    public static Properties errorCodeProp;
    // ===============================
    /**
     * 网址前缀
     */
    private static final String url_prex = "https://trade.chbtc.com";
    /**
     * 市场深度
     */
    private static final String url_depth = "http://api.chbtc.com/data/depth";
    /**
     * 用户资产信息查询
     */
    private static final String url_userInfo = "/api/getAccountInfo";
    /**
     * 挂单
     */
    private static final String url_trade = "/api/order";
    /**
     * 批量查询订单信息
     */
    private static final String url_batchQurey = "/api/getUnfinishedOrdersIgnoreTradeType";
    /**
     * 撤单
     */
    private static final String url_cancelOrder = "/api/cancelOrder";
    /** 批量下单的最大批量 */
    // private static final int max_batch_amount_trad = 5;
    /**
     * 批量查询订单的最大批量
     */
    private static final int max_batch_amount_queryOrder = 100;

    @Value("${chbtc.apiKey}")
    private String apiKey;
    @Value("${chbtc.secretKey}")
    private String secretKey;
    private static final double feeRate = 0.002;
    //------------------------

    /**
     * 市场买单系数
     */
    private final double buyRate = (1 - feeRate);
    /**
     * 市场卖单系数
     */
    private final double sellRate = (1 + feeRate);

    static {
        // 加载错误码配置
        errorCodeProp = new Properties();
        InputStream in = Trade_chbtc.class.getResourceAsStream("errorCode.properties");
        try {
            errorCodeProp.load(in);
            in.close();
        } catch (IOException e) {
            log.error("chbtc错误码文件加载异常", e);
        }
    }

    public Trade_chbtc(HttpUtil httpUtil, int platId, double usdRate,Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate,prop, engine);
        try {


            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();
        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }
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
            String jsonStr = httpUtil.requestHttpGet("", url_depth, null);
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
            changeMarketPrice(1 - feeRate,1 + feeRate);
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
            String paramStr = "method=getAccountInfo&accesskey=" + apiKey;
            String secret = EncryDigestUtil.digest(secretKey);
            String sign = EncryDigestUtil.hmacSign(paramStr, secret);
            paramStr += "&sign=" + sign + "&reqTime=" + System.currentTimeMillis();
            String jsonStr = httpUtil.requestHttpGet(url_prex, url_userInfo, paramStr);
            JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            AccountInfo accountInfo = new AccountInfo();
            double freeMoney = jsonObject.getJSONObject("result").getJSONObject("balance").getJSONObject("CNY").getDouble("amount");
            double freeGoods = jsonObject.getJSONObject("result").getJSONObject("balance").getJSONObject("BTC").getDouble("amount");
            double freezedMoney = jsonObject.getJSONObject("result").getJSONObject("frozen").getJSONObject("CNY").getDouble("amount");
            double freezedGoods = jsonObject.getJSONObject("result").getJSONObject("frozen").getJSONObject("BTC").getDouble("amount");
            accountInfo.setFreeMoney(freeMoney);
            accountInfo.setFreeGoods(freeGoods);
            accountInfo.setFreezedMoney(freezedMoney);
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
        changeMyOrderPrice(1 - feeRate,1 + feeRate);
        for (int orderCount = 0; orderCount < userOrderList.size(); orderCount++) {
            UserOrder order = userOrderList.get(orderCount);

            try {
                int tradeType = (order.getType().equals("buy") ? 1 : 0);

                // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
                double addPrice = (tradeType == 0 ? -1 * prop.huaDian2 : prop.huaDian2);
                String paramStr = "method=order&accesskey=" + apiKey + "&price=" + (order.getPrice()*(1 + addPrice)) + "&amount=" + (order.getVolume() - 0.00) + "&tradeType=" + tradeType
                        + "&currency=btc";
                String secret = EncryDigestUtil.digest(secretKey);
                String sign = EncryDigestUtil.hmacSign(paramStr, secret);
                paramStr += "&sign=" + sign + "&reqTime=" + System.currentTimeMillis();
                String jsonStr = httpUtil.requestHttpGet(url_prex, url_trade, paramStr);
                // 返回的对象
                JSONObject jsonObject = JSONObject.fromObject(jsonStr);
                String code = jsonObject.getString("code");
                // 如果挂单成功
                if (code.equals("1000")) {
                    // 设置orderId
                    order.setOrderId(jsonObject.getString("id"));
                    log.info(getPlatName() + "挂单成功:" + order + ",jsonStr:" + jsonStr);
                } else {// 挂单失败
                    order.disableOrder();
                    order.setOrderId("-1");
                    log.warn(getPlatName() + "挂单失败:" + code + errorCodeProp.getProperty(code) + order.toString());
                }
            } catch (Exception e) {
                // log.error(e.getMessage(), e);
                throw e;
            }


        }// end for


        // 删除挂单失败的、无效的
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
        for (UserOrder o : userOrderList) {
            o.setFinished(true);
        }
        // 分页查询,每循环一次代表一页
        for (int index = 1; ; index++) {
            String paramStr = "method=getUnfinishedOrdersIgnoreTradeType&accesskey=" + apiKey + "&currency=btc&pageIndex=" + index + "&pageSize=" + max_batch_amount_queryOrder;
            String secret = EncryDigestUtil.digest(secretKey);
            String sign = EncryDigestUtil.hmacSign(paramStr, secret);
            paramStr += "&sign=" + sign + "&reqTime=" + System.currentTimeMillis();
            String jsonStr = httpUtil.requestHttpGet(url_prex, url_batchQurey, paramStr);
            // 检查：如果返回的不是数组,表明发生了异常,就跳出循环
            char firstChar = jsonStr.charAt(0);
            if (firstChar != '[') {
                // JSONObject jsonObject = JSONObject.fromObject(jsonStr);
                // throw new Exception(getPlatName()+"订单状态查询发生异常：" +
                // jsonObject.getString("message") + ",json:" + jsonStr);
                log.error(getPlatName() + "订单查询问题：json:" + jsonStr + "结束查询");
                return 1;
            }
            // 得到返回的数组
            JSONArray jsonArray = JSONArray.fromObject(jsonStr);
            // 给没完成的订单设置状态
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonOrder = jsonArray.getJSONObject(i);
                UserOrder order = findOrderById(jsonOrder.getString("id"));
                if (order != null) {
                    order.setFinished(false);// 设为没完全成交
                    log.info(getPlatName() + "没完全成交:" + jsonOrder.toString());
                } else {
                    log.warn(getPlatName() + "出现意外的订单:" + jsonOrder.toString());
                    UserOrder tmpOrder = new UserOrder();
                    tmpOrder.setFinished(false);
                    tmpOrder.setOrderId(jsonOrder.getString("id"));
                    userOrderList.add(tmpOrder);
                }
            }
            // 如果本批次没满,就跳出循环
            if (jsonArray.size() < max_batch_amount_queryOrder) {
                break;
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
            String paramStr = "method=cancelOrder&accesskey=" + apiKey + "&id=" + order.getOrderId() + "&currency=btc";
            String secret = EncryDigestUtil.digest(secretKey);
            String sign = EncryDigestUtil.hmacSign(paramStr, secret);
            paramStr += "&sign=" + sign + "&reqTime=" + System.currentTimeMillis();
            String jsonStr = httpUtil.requestHttpGet(url_prex, url_cancelOrder, paramStr);
            // 返回的对象
            //JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            String msg = getPlatName() + "撤单返回消息：" + jsonStr;
            log.info(msg);

        }// end for

    }

    public String getPlatName() {
        return platName;
    }

    /**
     * 提取goods
     *
     * @throws Exception
     */
    @Override
    public void withdraw(String productName ,double amount, String address) throws Exception {
        throw new Exception("不支持提币");
    }

}

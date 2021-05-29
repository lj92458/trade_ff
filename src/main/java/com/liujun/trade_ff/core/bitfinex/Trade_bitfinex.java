package com.liujun.trade_ff.core.bitfinex;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
@Component
@Scope("prototype")
public class Trade_bitfinex extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_bitfinex.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");

    public static final String platName = "bitfinex";
    // 错误码
    public static Properties errorCodeProp;
    // ===============================
    /**
     * 网址前缀
     */
    private static final String url_prex = "https://api.bitfinex.com";
    /**
     * 市场深度
     */
    private static final String url_depth = "/v1/book/btcusd";
    /**
     * 用户资产信息查询
     */
    private static final String url_userInfo = "/v1/balances";
    /**
     * 挂单
     */
    private static final String url_trade = "/v1/order/new";
    /**
     * 查询活动的订单信息
     */
    private static final String url_batchQurey = "/v1/orders";
    /**
     * 撤单
     */
    private static final String url_cancelOrder = "/v1/order/cancel/multi";//批量撤单:/v1/order/cancel/multi

    /** 批量下单的最大批量 */
    // private static final int max_batch_amount_trad = 5;
    /**
     * 批量查询订单的最大批量
     */
    private static final int max_batch_amount_queryOrder = 100;

    /**
     * 交易手续费费率
     */
    private static final double feeRate = 0.002;
    @Value("${bitfinex.apiKey}")
    private String apiKey;
    @Value("${bitfinex.secretKey}")
    private String secretKey;

    //==================  对象属性  ======================

    private SignUtil signUtil;
    /**
     * 市场买单系数
     */
    private final double buyRate = (1 - feeRate) * usdRate;
    /**
     * 市场卖单系数
     */
    private final double sellRate = (1 + feeRate) * usdRate;
    //====================================================

    static {
        /*
		// 加载错误码配置
		errorCodeProp = new Properties();
		InputStream in = Trade_bitfinex.class.getResourceAsStream("errorCode.properties");
		try {
			errorCodeProp.load(in);
			in.close();
		} catch (IOException e) {
			log.error("bitfinex错误码文件加载异常", e);
		}
		*/
    }

    public Trade_bitfinex(HttpUtil httpUtil, int platId, double usdRate,Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate,prop, engine);
        try {
            if (usdRate == 0) {
                throw new Exception("汇率不能为0");
            }

            signUtil = new SignUtil(apiKey, secretKey);

            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();
        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询市场深度,填充marketDepth属性。Get /book/btcusd
     *
     * @throws Exception
     */
    public void flushMarketDeeps() throws Exception {
        // 初始化,清空
        MarketDepth depth = getMarketDepth();
        depth.getAskList().clear();
        depth.getBidList().clear();
        try {
            String jsonStr = signUtil.requestGet(url_prex + url_depth, "limit_bids=" + prop.marketOrderSize + "&limit_asks=" + prop.marketOrderSize);
            JSONObject jsonObject = JSONObject.fromObject(jsonStr);
            JSONArray askArr = jsonObject.getJSONArray("asks");
            JSONArray bidArr = jsonObject.getJSONArray("bids");
            // 卖方挂单
            for (int i = 0; i < askArr.size(); i++) {

                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(askArr.getJSONObject(i).getDouble("price"));
                marketOrder.setVolume(askArr.getJSONObject(i).getDouble("amount"));
                marketOrder.setPlatId(platId);

                depth.getAskList().add(marketOrder);

            }
            // 买方挂单
            for (int i = 0; i < bidArr.size(); i++) {
                MarketOrder marketOrder = new MarketOrder();// 一个挂单
                marketOrder.setPrice(bidArr.getJSONObject(i).getDouble("price"));
                marketOrder.setVolume(bidArr.getJSONObject(i).getDouble("amount"));
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
     * 查询账户资产信息 初始化时,需要查询账户信息。今后只有交易后,才需要重新查询。 /balances
     */
    public void flushAccountInfo() throws Exception {
        try {
            Map<String, Object> paramMap = new HashMap<String, Object>();

            String jsonStr = signUtil.request(url_prex + url_userInfo, url_userInfo, paramMap);
            log.debug(jsonStr);
            JSONArray jsonArray = JSONArray.fromObject(jsonStr);

            try {
                AccountInfo accountInfo = new AccountInfo();
                double freeMoney = 0;
                double freeGoods = 0;
                double freezedMoney = 0;
                double freezedGoods = 0;
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    if (jsonObj.getString("type").equals("exchange")) {
                        if (jsonObj.getString("currency").equals("btc")) {//currency
                            freeGoods = Double.parseDouble(jsonObj.getString("available"));//活动金额
                            freezedGoods = Double.parseDouble(jsonObj.getString("amount")) - freeGoods;//冻结金额
                        } else if (jsonObj.getString("currency").equals("usd")) {
                            freeMoney = Double.parseDouble(jsonObj.getString("available"));//活动金额
                            freezedMoney = Double.parseDouble(jsonObj.getString("amount")) - freeMoney;//冻结金额
                        }
                    }
                }

                accountInfo.setFreeMoney(freeMoney * usdRate);
                accountInfo.setFreeGoods(freeGoods);
                accountInfo.setFreezedMoney(freezedMoney * usdRate);
                accountInfo.setFreezedGoods(freezedGoods);
                setAccInfo(accountInfo);
            } catch (Exception e) {
                log.error(jsonStr);
                throw new Exception(e);
            }
        } catch (Exception e) {

            //log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }

    }

    /**
     * 各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号,然后删除挂单失败的 /order/new
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

            //int tradeType = (order.getType().equals("buy") ? 1 : 0);
            // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
            double addPrice = (order.getType().equals("buy") ? prop.huaDian2 : -1 * prop.huaDian2);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put("symbol", "btcusd");
            paramMap.put("amount", order.getVolume() - 0.01);
            paramMap.put("price", new Double(order.getPrice()*(1 + addPrice)));
            paramMap.put("exchange", "bitfinex");
            paramMap.put("side", order.getType());
            paramMap.put("type", "exchange limit");
            paramMap.put("is_hidden", "false");

            try {

                String jsonStr = signUtil.request(url_prex + url_trade, url_trade, paramMap);
                log.info("tradeOrder jsonStr:" + jsonStr);
                // 返回的对象
                JSONObject jsonObject = JSONObject.fromObject(jsonStr);
                try {
                    // 设置orderId
                    order.setOrderId("" + jsonObject.getInt("order_id"));
                    log.info(getPlatName() + "挂单成功:" + order + ",jsonStr:" + jsonStr);

                } catch (Exception e) {//挂单失败
                    log.error(getPlatName() + "挂单失败:" + order + ",jsonStr:" + jsonStr);
                    order.disableOrder();
                    order.setOrderId("-1");
                }
            } catch (Exception e) {
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
     * 查出没完全成交的订单. /orders
     */
    @Override
    public int queryOrderState() throws Exception {
        List<UserOrder> userOrderList = getUserOrderList();
        // 先将订单默认设为“已完全成交”
        for (UserOrder o : userOrderList) {
            o.setFinished(true);
        }
        Map<String, Object> paramMap = new HashMap<String, Object>();
        String jsonStr = signUtil.request(url_prex + url_batchQurey, url_batchQurey, paramMap);
        log.info("query order jsonstr:" + jsonStr);
        try {
            // 得到返回的数组
            JSONArray jsonArray = JSONArray.fromObject(jsonStr);
            // 给没完成的订单设置状态
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonOrder = jsonArray.getJSONObject(i);
                UserOrder order = findOrderById(jsonOrder.getInt("order_id") + "");
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

            int unFinishedNum = 0;//统计没成交的订单
            for (int i = userOrderList.size() - 1; i >= 0; i--) {
                UserOrder order = userOrderList.get(i);
                if (!order.isFinished()) {
                    unFinishedNum++;
                }
            }// end for
            return unFinishedNum;
        } catch (Exception e) {
            throw new Exception("jsonStr", e);
        }
    }

    /**
     * 撤销没完全成交的订单 /v1/order/cancel/multi
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
        StringBuilder jsonStrBuilder = new StringBuilder('[');
        for (UserOrder order : userOrderList) {//url_cancelOrder
            if (jsonStrBuilder.length() > 1) {
                jsonStrBuilder.append(',');
            }
            jsonStrBuilder.append(order.getOrderId());
        }
        jsonStrBuilder.append(']');
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("order_ids", jsonStrBuilder.toString());
        String jsonStr = signUtil.request(url_prex + url_cancelOrder, url_cancelOrder, paramMap);
        log.info("cancelOrder jsonstr:" + jsonStr);
        //TODO

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

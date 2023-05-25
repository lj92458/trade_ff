package com.liujun.trade_ff.core.okcoin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import com.okex.open.api.bean.funding.param.FundsTransfer;
import com.okex.open.api.bean.funding.param.Withdrawal;
import com.okex.open.api.bean.trade.param.CancelOrder;
import com.okex.open.api.bean.trade.param.PlaceOrder;
import com.okex.open.api.config.APIConfiguration;
import com.okex.open.api.enums.I18nEnum;
import com.okex.open.api.service.account.AccountAPIService;
import com.okex.open.api.service.account.impl.AccountAPIServiceImpl;
import com.okex.open.api.service.funding.FundingAPIService;
import com.okex.open.api.service.funding.impl.FundingAPIServiceImpl;
import com.okex.open.api.service.marketData.MarketDataAPIService;
import com.okex.open.api.service.marketData.impl.MarketDataAPIServiceImpl;
import com.okex.open.api.service.trade.TradeAPIService;
import com.okex.open.api.service.trade.impl.TradeAPIServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 第五版api https://github.com/CollmeYH/okex-java-sdk-api-v5
 * api在线体验(swagger) https://www.okx.com/cn/demo-trading-explorer/v5/zh
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
public class Trade_okcoin extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_okcoin.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
    public static final String platName = "okcoin";


    private MarketDataAPIService marketDataAPIService;
    private AccountAPIService accountAPIService;
    private TradeAPIService tradeAPIService;
    private FundingAPIService fundingAPIService;
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
    private String coinPair;
    //------------------------


    public Trade_okcoin(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        super(httpUtil, platId, usdRate, prop, engine);


    }

    @PostConstruct
    private void init() {
        // ===============================
        APIConfiguration config = new APIConfiguration();
        config.setEndpoint(url_prex);
        config.setApiKey(apiKey);
        config.setSecretKey(secretKey);
        config.setPassphrase(passphrase);

        config.setPrint(false);
        config.setI18n(I18nEnum.SIMPLIFIED_CHINESE);
        this.marketDataAPIService = new MarketDataAPIServiceImpl(config);
        this.tradeAPIService = new TradeAPIServiceImpl(config);
        this.accountAPIService = new AccountAPIServiceImpl(config);
        this.fundingAPIService = new FundingAPIServiceImpl(config);
        coinPair = token[0] + "-" + token[1];
        try {
            // 初始查询账户信息。今后只有交易后,才需要重新查询。
            flushAccountInfo();
            flushMarketDeeps();
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
        ArrayList<MarketOrder>[] depth = getMarketDepth();
        try {
            JSONObject bookJson = marketDataAPIService.getOrderBook(coinPair, prop.marketOrderSize + "").getJSONArray("data").getJSONObject(0);
            for (int i = 0; i < 2; i++) {
                depth[i].clear();
                JSONArray[] arrArr = new JSONArray[]{bookJson.getJSONArray("asks"), bookJson.getJSONArray("bids")};
                JSONArray orderArr = arrArr[i];
                for (Object obj : orderArr) {
                    depth[i].add(new MarketOrder(
                            platId,
                            Double.parseDouble(((JSONArray) obj).getString(0)),
                            Double.parseDouble(((JSONArray) obj).getString(1))
                    ));
                }
            }
            sort(depth);// 排序
            changeMarketPrice(1 - feeRate, 1 + feeRate);
            backupUsefulOrder();
            // 设置当前价格
            double askPrice = depth[0].get(0).getPrice();
            double bidPrice = depth[1].get(0).getPrice();
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
            //查询账户信息  https://www.okx.com/docs-v5/zh/#rest-api-account-get-balance
            JSONArray arr = accountAPIService.getBalance(token[0] + "," + token[1]).getJSONArray("data").getJSONObject(0).getJSONArray("details");
            JSONObject[] accArr = new JSONObject[]{arr.getJSONObject(0), arr.getJSONObject(1)};
            if (accArr[0].getString("ccy").equals(token[1])) {//两个对象可能要交换
                JSONObject tmp = accArr[0];
                accArr[0] = accArr[1];
                accArr[1] = tmp;
            }
            for (int i = 0; i < 2; i++) {
                //cashBal(币种余额)= availBal(可用余额) + frozenBal(被占用金额)　？　ordFrozen(挂单冻结数量)又是什么？
                accountInfo.freeToken[i] = Double.parseDouble(accArr[i].getString("availBal"));
                accountInfo.freezedToken[i] = Double.parseDouble(accArr[i].getString("frozenBal"));
                accountInfo.totalToken[i] = accountInfo.freeToken[i] + accountInfo.freezedToken[i];
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
        List<PlaceOrder> batch = null;
        List<List<PlaceOrder>> batchList = new ArrayList<>();
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
            order.setFinished(true);//默认都成交了。只因okex平台只能查出没成交的，所以我们必须默认成交了。

            // 如果新的批次开始,就结束前面批次
            if (0 == orderCount % max_batch_amount_trad) {
                if (orderCount != 0) {// 如果前面有批次
                    batchList.add(batch);
                }
                batch = new ArrayList<>();
            }

            // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
            double addPrice = (order.getType().equals("sell") ? -1 * prop.huaDian2 : prop.huaDian2);

            PlaceOrder orderParam = new PlaceOrder();
            orderParam.setInstId(coinPair);
            orderParam.setTdMode("cash");
            orderParam.setPx(Prop.fmt_money.get().format(order.getPrice() * (1 + addPrice)));//即然决定用市价成交，那么price是无效的
            orderParam.setOrdType("limit");//todo market limit哪个更好？
            orderParam.setSide(order.getType());
            orderParam.setSz(Prop.fmt_goods.get().format(order.getVolume() - 0.00));
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
            JSONObject orderResult = this.tradeAPIService.placeMultipleOrders(batchList.get(i));
            JSONArray orderResultArr = orderResult.getJSONArray("data");
            if (orderResultArr == null || orderResultArr.size() == 0) {
                throw new Exception("下单返回结果为空： " + JSON.toJSONString(orderResult));
            }
            for (int j = 0; j < orderResultArr.size(); j++) {
                // 当前订单在userOrderList中的下标
                int orderIndex = i * max_batch_amount_trad + j;
                JSONObject resultObj = (JSONObject) orderResultArr.get(j);
                // 设置orderId
                UserOrder thisOrder = userOrderList.get(orderIndex);
                if (thisOrder == null) {
                    throw new Exception(getPlatName() + "获取本地订单异常：null,index:" + orderIndex + ",size:" + userOrderList.size());
                }
                log.info(getPlatName() + " ???????????本批次第" + j + "个订单是" + thisOrder + "吗? 总index" + orderIndex);
                thisOrder.setOrderId(resultObj.getString("ordId"));
                if (!resultObj.getString("sCode").equals("0")) {
                    log.error(getPlatName() + "下单失败：" + resultObj.getString("sMsg"));
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
        long after = Long.parseLong(userOrderList.get(userOrderList.size() - 1).getOrderId()) + 1;
        //找到最小订单号的前面那个订单号，作为before参数。防止查出早期的旧订单
        long before = Long.parseLong(userOrderList.get(0).getOrderId()) - 1;
        // 查询没成交的
        JSONObject resultObj = tradeAPIService.getOrderList("SPOT", null, coinPair, null, null, "" + after, "" + before, "100");
        JSONArray jsonArray = resultObj.getJSONArray("data");
        for (Object o : jsonArray) {
            JSONObject jsonO = (JSONObject) o;
            String orderId = jsonO.getString("ordId");
            UserOrder order = findOrderById(orderId);
            if (order != null) {
                order.setFinished(false);//查出来的，都是没成交的，那么查不出来的，就是成交了
                log.info(getPlatName() + "订单没成交:" + jsonO.toJSONString());
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
        log_haveTrade.info("okcion++++++++++++++至少赚了" + prop.formatMoney(haveEarn) + ". 完全成交" + finishedList.size() + "个订单：" + finishedList);

        // userOrderList里面剩下的是没完全成交的,全部撤单。一次最多撤10个
        List<CancelOrder> cancleOrders = new ArrayList<>();

        List<String> order_ids = new ArrayList<>();
        if (userOrderList.size() > 0) {
            for (UserOrder userOrder : userOrderList) {
                CancelOrder o = new CancelOrder();
                o.setInstId(coinPair);
                o.setOrdId(userOrder.getOrderId());
                cancleOrders.add(o);
            }
            this.tradeAPIService.cancelMultipleOrders(cancleOrders);
        }
    }

    public String getPlatName() {
        return platName;
    }

    /**
     * 提取资产，发送token到外界.为什么一定要等待，直到被打包呢？因为要拿到哈希值。有了哈希值，才能调用nodeJS的receiveToken服务，进而把eth包装成weth。
     *
     * @param productName
     * @param amount
     * @param address     必须是认证过的地址
     * @param needWrap    只有dex需要。当dex被要求发送eth而不是weth，needWrap应该为true，这样就能把weth变成eth并发送。当dex被要求发送weth, needWrap却还是设为true,就会把eth转成weth并发送(这好像没什么意义)
     * @return txId 交易哈希
     * @throws Exception
     */
    @Override
    public String withdraw(String productName, double amount, String address, String netWorkShort, boolean needWrap) throws Exception {
        JSONArray jsonArr = this.fundingAPIService.getCurrencies(productName).getJSONArray("data");//如果要查询多个币种，用逗号分隔
        JSONObject ccy = null;
        String chain = null;//网络名称
        for (Object o : jsonArr) {//从多条链中找到我们要的链
            ccy = (JSONObject) o;
            chain = ccy.getString("chain");
            //china名称举例：ETH-ERC20， ETH-Arbitrum one，ETHK-OKTC， ETH-Optimism，ETH-zkSync Lite
            //USDC-ERC20, USDC-Arbitrum one, USDC-OKTC, USDC-Polygon, USDC-Optimism, USDC-TRC20, USDC-Avalanche C-Chain
            if (chain.toUpperCase().contains(netWorkShort.toUpperCase()))
                break;
            else ccy = null;
        }
        if (ccy == null) {
            throw new Exception(productName + " 不存在于指定网络:" + tokenNetWork[0] + "_" + tokenNetWork[1] + ", 可选的网络是：" + jsonArr.toJSONString());
        }

        //如果资金账户余额不足，就从交易账户划转
        double financeAmount = queryFinanceAmount(productName);
        if (financeAmount < amount) {
            //先把资金从现货账户，划转到资金账户，才能提走。
            FundsTransfer fundsTransfer = new FundsTransfer();
            fundsTransfer.setCcy(productName);
            String transAmount = prop.transTokenFromat.format((amount - financeAmount) * 1.0001);//多增加万分之一，防止浮点数误差带来的失败
            if (Double.parseDouble(transAmount) > amount) {//如果扩大万分之一，导致超过原来的值，就要用原来的值
                transAmount = prop.transTokenFromat.format(amount);
            }
            fundsTransfer.setAmt(transAmount);//6：资金账户 18：交易账户
            fundsTransfer.setFrom("18");
            fundsTransfer.setTo("6");
        /*划转类型,默认是0.  0账户内划转 1母账户转子账户(仅适用于母账户APIKey)  2子账户转母账户(仅适用于母账户APIKey) 3子账户转母账户(仅适用于子账户APIKey)
          4子账户转子账户(仅适用于子账户APIKey，且目标账户需要是同一母账户下的其他子账户).*/
            fundsTransfer.setType("0");

            JSONObject transResult = fundingAPIService.fundsTransfer(fundsTransfer);
            if (!transResult.getString("code").equals("0")) {
                log.info("资金划转失败， " + transResult.toJSONString());
                return "";
            } else {
                log.info("资金账户余额不足，已划转" + transAmount + ",等待到账....");
            }
            double financeAmount2 = 0;
            for (int i = 0; i < 10; i++) {
                Thread.sleep(2000);//等2秒
                financeAmount2 = queryFinanceAmount(productName);
                if (financeAmount2 >= amount) {
                    log.info("资金划转已到账，当前余额" + financeAmount2);
                    break;
                } else {
                    log.info("资金账户当前余额" + financeAmount2 + ", 等待划转到账");
                }
            }//end for
            if (financeAmount2 < amount) {//如果还是不行，就没办法了
                return "";
            }
        }

        //开始从资金账户提币
        Withdrawal w = new Withdrawal();
        w.setCcy(productName);
        w.setAmt("" + amount);
        w.setDest("4");
        w.setToAddr(address);//必须是认证过的地址
        //手续费取中间值。不用格式化成6位小数吧？
        double fee = (Double.parseDouble(ccy.getString("minFee")) + Double.parseDouble(ccy.getString("maxFee"))) / 2.0;
        w.setFee(prop.transTokenFromat.format(fee));
        w.setChain(chain);
        log.info("提币请求" + w);
        JSONObject jsonObject = this.fundingAPIService.Withdrawal(w);
        log.info("提币结果：" + jsonObject.toJSONString());
        if (!jsonObject.getString("code").equals("0")) {
            return "";
        }
        JSONObject drawResult = (JSONObject) jsonObject.getJSONArray("data").get(0);
        if (drawResult.getString("wdId") == null) {
            return "";
        }
        //轮番查询状态，直到返回链上交易哈希tranId. 最多等10分钟
        int sleepSecond = 3;
        for (int i = 0; i < 10 * 60 / sleepSecond; i++) {
            Thread.sleep(1000 * sleepSecond);//等3秒
            try {
                JSONObject queryResult = (JSONObject) this.fundingAPIService.getWithdrawalHistory(null, null, null, null, null, drawResult.getString("wdId"), null, null)
                        .getJSONArray("data").get(0);
                if (queryResult != null && queryResult.getString("state") != null && queryResult.getString("state").equals("2")) {
                    log.info("okx提币已到账" + ", txId=" + queryResult.getString("txId"));
                    return queryResult.getString("txId");
                } else {
                    if (queryResult != null && queryResult.getString("state") != null) {
                        log.info("等待okx提现到账，state=" + queryResult.getString("state") + ", 含义：-3撤销中，-2已撤销-1失败，0等待提币，1提币中，2提币成功，7: 审核通过，10: 等待划转，［4, 5, 6, 8, 9, 12］等待客服审核");
                    } else {
                        log.info("等待okx提现到账，queryResult=" + queryResult);
                    }
                }
            } catch (Exception e) {
                log.error("fundingAPIService.getWithdrawalHistory异常：", e);
            }
        }//end for
        return "";
    }

    private double queryFinanceAmount(String productName) {
        JSONObject balanceObj = fundingAPIService.getBalance(productName);
        if (!balanceObj.getString("code").equals("0")) {
            log.info("查询【资金账户余额】失败， " + balanceObj.toJSONString());
            return 0;
        } else {
            log.info("资金账户余额：" + balanceObj.toJSONString());
        }
        return Double.parseDouble(((JSONObject) balanceObj.getJSONArray("data").get(0)).getString("availBal"));
    }

    public double depositToken(String asset, String txId, double amount, boolean needWrap) throws Exception {
        //轮番查询状态，直到返回链上交易哈希tranId. 最多等10分钟
        int sleepSecond = 3;//每三秒查询一次
        for (int i = 0; i < 10 * 60 / sleepSecond; i++) {
            Thread.sleep(1000 * sleepSecond);
            try {
                JSONObject queryResult = fundingAPIService.getDepositHistory(null, null, null, null, null, txId).getJSONArray("data").getJSONObject(0);
                if (queryResult != null && queryResult.getString("state").equals("2")) {
                    log.info("okx充值已到账" + ", 确认次数actualDepBlkConfirm=" + queryResult.getString("actualDepBlkConfirm"));
                    return Double.parseDouble(queryResult.getString("amt"));
                } else {
                    if (queryResult != null) {
                        log.info("等待okx充值到账，stat3=" + queryResult.getString("state") + ", 确认次数actualDepBlkConfirm=" + queryResult.getString("actualDepBlkConfirm"));
                    } else {
                        log.info("等待okx充值到账，queryResult=null");
                    }
                }
            } catch (Exception e) {
                log.error("fundingAPIService.getDepositHistory异常：", e);
            }
        }//end for
        return -1;
    }

    @Value("${okcoin.goods}")
    public void setGoods(String goods) {
        token[0] = goods.toUpperCase();
    }

    @Value("${okcoin.money}")
    public void setMoney(String money) {
        token[1] = money.toUpperCase();
    }

    @Value("${okcoin.goodsAddress}")
    public void setGoodsAddress(String goodsAddress) {
        tokenAddress[0] = goodsAddress;
    }

    @Value("${okcoin.moneyAddress}")
    public void setMoneyAddress(String moneyAddress) {
        tokenAddress[1] = moneyAddress;
    }

    @Value("${okcoin.goodsNetWork}")
    public void setGoodsNetWork(String goodsNetWork) {
        tokenNetWork[0] = StringUtils.isEmpty(goodsNetWork) ? new String[0] : goodsNetWork.split(",");
    }

    @Value("${okcoin.moneyNetWork}")
    public void setMoneyNetWork(String moneyNetWork) {
        tokenNetWork[1] = StringUtils.isEmpty(moneyNetWork) ? new String[0] : moneyNetWork.split(",");
    }
}

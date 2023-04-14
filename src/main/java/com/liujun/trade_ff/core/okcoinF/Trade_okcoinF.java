package com.liujun.trade_ff.core.okcoinF;
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.liujun.trade_ff.core.Engine;
//import com.liujun.trade_ff.core.Prop;
//import com.liujun.trade_ff.core.Trade;
//import com.liujun.trade_ff.core.modle.AccountInfo;
//import com.liujun.trade_ff.core.modle.MarketDepth;
//import com.liujun.trade_ff.core.modle.MarketOrder;
//import com.liujun.trade_ff.core.modle.UserOrder;
//import com.liujun.trade_ff.core.util.HttpUtil;
//import com.okcoin.commons.okex.open.api.bean.futures.param.Order;
//import com.okcoin.commons.okex.open.api.bean.futures.result.Book;
//import com.okcoin.commons.okex.open.api.bean.futures.result.Instruments;
//import com.okcoin.commons.okex.open.api.bean.futures.result.OrderResult;
//import com.okcoin.commons.okex.open.api.config.APIConfiguration;
//import com.okcoin.commons.okex.open.api.enums.I18nEnum;
//import com.okcoin.commons.okex.open.api.service.futures.FuturesMarketAPIService;
//import com.okcoin.commons.okex.open.api.service.futures.FuturesTradeAPIService;
//import com.okcoin.commons.okex.open.api.service.futures.impl.FuturesMarketAPIServiceImpl;
//import com.okcoin.commons.okex.open.api.service.futures.impl.FuturesTradeAPIServiceImpl;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Scope;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 知识背景：
// * 1.花钱购买合约，这个钱不是真的花出去了，而是作为保证金，暂时被冻结了，但还是属于你的。
// * 2.保证金：用来购买合约的资金，才叫保证金。例如花了60元，用了10倍杠杆,买到面额600元的合约。这个60元就是保证金。 账户剩余的钱，不叫保证金。
// * 3.保证金=持仓量/杠杆倍数
// * 4.账户余额：账户上真实存在的资金，包含了保证金在内。这是最重要最基础的概念，其它概念都是衍生的
// * 5.账户权益：余额+浮盈-浮亏
// * 6.可用保证金：权益-不可用保证金(持仓/挂单)*
// * <p>
// * 7.合约交易平台，资金既能用来买(做多)，又能用来卖(做空)，所以认为这个资金既是钱又是货。
// * 账户的购买力(钱货)=余额*最大仓位占比 -保证金*杠杆倍数。如果做多，认为钱货都减少，货增加(因为持有多仓)。也就是：货不变，钱减少。
// * 如果满仓做多，那么货不变，钱归零。
// * <p>
// * 8.仓位计算(币本位合约)
// * 假设你账户余额是a，持仓量b，价格跌了x就爆仓，例如x=0.5代表50%
// * 那么爆仓时，余额的价值等于你亏损的价值，也就是你的钱刚好够还债：a(1-x)=bx, 所以x=a/(b+a), b=a(1-x)/x
// * 如果价格涨了x就爆仓呢？则a(1+x)=bx，所以x=a/(b-a),  b=a(1+x)/x
// * 如果你打算涨50%爆仓，跌50%爆仓。做多可持仓a，做空可持仓3a。发现了吗？币本位合约对做空有利，能持有更大的仓位。
// */
//@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
//@Component
//@Scope("prototype")
//public class Trade_okcoinF extends Trade {
//    private static final Logger log = LoggerFactory.getLogger(Trade_okcoinF.class);
//    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
//    public static final String platName = "okcoinF";
//
//
//    // ===============================
//    private APIConfiguration config;
//    public FuturesMarketAPIService futuresMarketAPIService;
//    public FuturesTradeAPIService futuresTradeAPIService;
//
//    private Instrument instrument;
//    /**
//     * 网址前缀
//     */
//    @Value("${okcoin.url}")
//    private String url_prex;
//    @Value("${okcoin.passphrase}")
//    private String passphrase;//解密密码
//    /**
//     * 批量下单的最大批量
//     */
//    private int max_batch_amount_trad = 10;
//
//
//    @Value("${okcoin.apiKey}")
//    private String apiKey;
//    @Value("${okcoin.secretKey}")
//    private String secretKey;
//    @Value("${okcoinF.feeRate}")
//    private double feeRate;
//    private String coinPair;
//    @Value("${okcoinF.underlying}")
//    public String underlying;
//    @Value("${okcoinF.alias}")
//    private String alias;
//    //------------------------
//
//
//    public Trade_okcoinF(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
//        super(httpUtil, platId, usdRate, prop, engine);
//
//
//    }
//
//    @PostConstruct
//    private void init() {
//        this.config = new APIConfiguration();
//        config.setEndpoint(url_prex);
//        config.setApiKey(apiKey);
//        config.setSecretKey(secretKey);
//        config.setPassphrase(passphrase);
//
//        config.setPrint(false);
//        config.setI18n(I18nEnum.SIMPLIFIED_CHINESE);
//        this.futuresMarketAPIService = new FuturesMarketAPIServiceImpl(this.config);
//        this.futuresTradeAPIService = new FuturesTradeAPIServiceImpl(this.config);
//
//        String money2 = prop.money.endsWith("btc") ? "btc" : prop.money;
//        coinPair = prop.goods.toUpperCase() + "-" + money2.toUpperCase();
//        try {
//            // 初始查询账户信息。今后只有交易后,才需要重新查询。
//            flushAccountInfo();
//
//        } catch (Exception e) {
//
//            log.error(getPlatName() + " : " + e.getMessage(), e);
//        }
//
//        initSuccess = true;
//    }
//
//    /**
//     * 查询市场深度,填充marketDepth属性。Get   https://www.okex.com/api/futures/v3/instruments
//     * {"instrument_id":"BTC-USD-210108","underlying_index":"BTC","quote_currency":"USD","tick_size":"0.01",
//     * "contract_val":"100","listing":"2020-12-25","delivery":"2021-01-08","trade_increment":"1","alias":"this_week",
//     * "underlying":"BTC-USD","base_currency":"BTC","settlement_currency":"BTC","is_inverse":"true",
//     * "contract_val_currency":"USD","category":"1"}
//     *
//     * @throws Exception
//     */
//    public void flushMarketDeeps() throws Exception {
//        // 初始化,清空
//        MarketDepth depth = getMarketDepth();
//        depth.getAskList().clear();
//        depth.getBidList().clear();
//        try {
//            Book book = futuresMarketAPIService.getInstrumentBook(instrument.getInstrument_id(), prop.marketOrderSize + "", "" + prop.orderStepLength);
//            //同时存储买单和卖单
//            List[] localListArr = {depth.getAskList(),depth.getBidList()};
//            JSONArray[] jsonArrayArr = {book.getAsks(), book.getBids()};
//            for (int j = 0; j < 2; j++) {//为了避免重复的代码。就用一个for循环处理买单和卖单
//                JSONArray orderArr = jsonArrayArr[j];
//                for (int i = 0; i < orderArr.size(); i++) {
//                    MarketOrder marketOrder = new MarketOrder();// 一个挂单
//                    JSONArray oneOrder = orderArr.getJSONArray(i);
//                    marketOrder.setPrice(Double.parseDouble(oneOrder.getString(0)));
//                    double contractCount = Double.parseDouble(oneOrder.getString(1));//合约有多少张
//                    double volume = 0;
//                    //面额是美元
//                    if (instrument.getContract_val_currency().equals("USD")) {
//                        volume = contractCount * instrument.getContract_val() / marketOrder.getPrice();
//
//                    } else {//面额是btc
//                        volume = contractCount * instrument.getContract_val();
//                    }
//                    marketOrder.setVolume(volume);
//                    marketOrder.setPlatId(platId);
//
//                    localListArr[j].add(marketOrder);
//                }//end for i
//
//            }//end for j
//            sort(depth);// 排序
//            changeMarketPrice(1 - feeRate, 1 + feeRate);
//            backupUsefulOrder();
//            // 设置当前价格
//            double askPrice = depth.getAskList().get(0).getPrice();
//            double bidPrice = depth.getBidList().get(0).getPrice();
//            setCurrentPrice((bidPrice + askPrice) / 2.0);
//            //
//        } catch (Exception e) {
//            // log.error(getPlatName()+"" + e.getMessage());
//            throw e;
//        }
//    }
//
//    /**
//     * 查询账户资产信息 .Post. 初始化时,需要查询账户信息。今后只有交易后,才需要重新查询。
//     * 合约交易平台，资金既能用来买(做多)，又能用来卖(做空)，所以认为这个资金既是钱又是货。
//     * 账户的购买力(钱货)=最大仓位-已消耗仓位=余额*最大仓位率 -保证金*杠杆倍数。如果做多，钱货减少，货加。【也就是：货不变，钱减少】。
//     * 如果做多满仓，那么货不变，钱归零。
//     */
//    public void flushAccountInfo() throws Exception {
//        try {
//            this.instrument = getInstrument(underlying, alias);
//            double contractVal = instrument.getContract_val();
//            //查出现有仓位，看当前是在做多还是做空。
//            JSONObject position = futuresTradeAPIService.getInstrumentPosition(instrument.getInstrument_id())
//                    .getJSONArray("holding").getJSONObject(0);
//            double leverage = Double.parseDouble(position.getString("leverage"));//杠杆倍数
//            double long_qty = Double.parseDouble(position.getString("long_qty"));//多仓合约张数
//            double short_qty = Double.parseDouble(position.getString("short_qty"));//空仓合约张数
//            double lastPrice = Double.parseDouble(position.getString("last"));//最新成交价
//            //设置仓位状态。空仓还是持仓
//            if (long_qty + short_qty == 0) {
//                engine.setFutureState(Engine.FUTURE_EMPTY);
//            } else {
//                engine.setFutureState(Engine.FUTURE_HOLD);
//            }
//            AccountInfo accountInfo = new AccountInfo();
//            JSONObject accountJO = futuresTradeAPIService.getAccountsByUnderlying(underlying);
//            double total = Double.parseDouble(accountJO.getString("total_avail_balance"));//余额(总额)
//            double equity = Double.parseDouble(accountJO.getString("equity"));//权益
//            double margin = Double.parseDouble(accountJO.getString("margin"));//保证金
//            //如果是币本位合约，余额是货
//            if (accountJO.getString("currency").equalsIgnoreCase(prop.goods)) {
//                //freeMoney=权益(或者余额)*持仓率-多仓仓位,   freeGoods=权益(或者余额)*持仓率-空仓仓位
//                //freezedMoney，根据持仓量表示的一个参数。用来检测平衡：确保钱恒定。做空，产生钱，消耗货。做多，消耗钱，产生货
//                //如果面额是美元
//                if (instrument.getContract_val_currency().equals("USD")) {
//                    accountInfo.setFreeMoney(equity * prop.positionRate * lastPrice - long_qty * contractVal);
//                    accountInfo.setFreeGoods(equity * prop.positionRate - short_qty * contractVal / lastPrice);
//                    accountInfo.setFreezedMoney((short_qty - long_qty) * contractVal);
//                    accountInfo.setFreezedGoods((long_qty - short_qty) * contractVal / lastPrice);
//                } else {//面额是btc
//                    accountInfo.setFreeMoney(equity * prop.positionRate * lastPrice - long_qty * contractVal * lastPrice);
//                    accountInfo.setFreeGoods(equity * prop.positionRate - short_qty * contractVal);
//                    accountInfo.setFreezedMoney((short_qty - long_qty) * contractVal * lastPrice);
//                    accountInfo.setFreezedGoods((long_qty - short_qty) * contractVal);
//                }
//                accountInfo.setTotalMoney(accountInfo.getFreezedMoney());
//                accountInfo.setTotalGoods(equity);
//            }
//
//            //如果是usdt本位合约,余额是钱
//            if (accountJO.getString("currency").equalsIgnoreCase(prop.money)) {
//                //如果面额是美元
//                if (instrument.getContract_val_currency().equals("USD")) {
//                    accountInfo.setFreeMoney(equity * prop.positionRate - long_qty * contractVal);
//                    accountInfo.setFreeGoods(equity * prop.positionRate / lastPrice - short_qty * contractVal / lastPrice);
//                    accountInfo.setFreezedMoney((short_qty - long_qty) * contractVal);
//                    accountInfo.setFreezedGoods((long_qty - short_qty) * contractVal / lastPrice);
//                } else {//面额是btc
//                    accountInfo.setFreeMoney(equity * prop.positionRate - long_qty * contractVal * lastPrice);
//                    accountInfo.setFreeGoods(equity * prop.positionRate / lastPrice - short_qty * contractVal);
//                    accountInfo.setFreezedMoney((short_qty - long_qty) * contractVal * lastPrice);
//                    accountInfo.setFreezedGoods((long_qty - short_qty) * contractVal);
//                }
//                accountInfo.setTotalMoney(equity);
//                accountInfo.setTotalGoods(accountInfo.getFreezedGoods());
//            }
//
//            //
//            setAccInfo(accountInfo);
//
//
//        } catch (Exception e) {
//            log.error(getPlatName() + " : " + e.getMessage(), e);
//            throw e;
//        }
//
//    }
//
//    /**
//     * 各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号,然后删除挂单失败的
//     */
//    public int tradeOrder() throws Exception {
//        log.info(getPlatName() + "开始下单");
//
//        List<UserOrder> userOrderList = getUserOrderList();
//        int orderCount = 0;// 有效订单的数量
//        // 删掉无效订单
//        for (int i = userOrderList.size() - 1; i >= 0; i--) {
//            if (!userOrderList.get(i).isEnable()) {
//                userOrderList.remove(i);// 无效订单要及时删掉，否则help_tradeOneBatch里面定位错误。但是不能在预处理时删。
//            }
//        }// end for
//        merge();//对订单进行合并
//        changeMyOrderPrice(1 - feeRate, 1 + feeRate);
//        for (; orderCount < userOrderList.size(); orderCount++) {
//            UserOrder order = userOrderList.get(orderCount);
//            int contractCount = 0;//需要执行的订单，包含多少张合约
//            //面额是美元
//            if (instrument.getContract_val_currency().equals("USD")) {
//                contractCount = (int) Math.round(order.getVolume() * order.getPrice() / instrument.getContract_val());
//
//            } else {//面额是btc
//                contractCount = (int) Math.round(order.getVolume() / instrument.getContract_val());
//            }
//
//
//            // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
//            double addPrice = 0;// (order.getType().equals("sell") ? -1 * prop.huaDian2 : prop.huaDian2);
//            Order o = new Order();
//            //o.setPrice(Double.toString(order.getPrice() * (1 + addPrice)));
//            o.setInstrument_id(instrument.getInstrument_id());
//            int qty = 0;//当前持仓数量(合约张数)。（要么都平仓，要么都不平，这样好么？不好）
//            ;//系统状态
//            //如果无仓，说明要开仓
//            log.info("挂单前的状态：engine.getFutureState():" + engine.getFutureState());
//            if (engine.getFutureState().equals(Engine.FUTURE_EMPTY)) {
//                if (order.getType().equals("buy")) {
//                    o.setType("1");//开多
//                } else {
//                    o.setType("2");//开空
//                }
//            } else {//如果有仓位,说明要平仓，还是加仓？
//                //查出现有仓位，看当前是在做多还是做空。
//                JSONObject position = futuresTradeAPIService.getPositionByInstrumentId(instrument.getInstrument_id())
//                        .getJSONArray("holding").getJSONObject(0);
//                if (order.getType().equals("buy")) {//有空就平空，如果还没吃完这个单，就做多。还有能力继续做多吗？肯定有：有多仓仓位，又有买单没过滤掉，说明多仓没满
//                    //freeMoney=权益(或者余额)*持仓率-多仓仓位
//                    qty = Integer.parseInt(position.getString("short_qty"));
//                    if (qty == 0) {//无空,有多
//                        o.setType("1");//开多
//                    } else {//有空，就平空。如果空不够用，也平空,剩余的开多
//                        o.setType("4");//平空
//                        //需要做多的量=订单量-空仓量
//                        double upQty = contractCount - qty;
//
//                        if (upQty > 0) {//如果有必要做多
//                            o.setSize("" + qty);
//                            log.info("空仓不够用，需要做多");
//                            Order oUp = new Order();
//                            //oUp.setPrice(Double.toString(order.getPrice() * (1 + addPrice)));
//                            oUp.setInstrument_id(instrument.getInstrument_id());
//                            oUp.setType("1");
//                            oUp.setSize("" + upQty);
//                            oUp.setOrder_type("4");//市价成交
//                            OrderResult result = futuresTradeAPIService.order(oUp);
//                            log.info("多单下单结果" + result.getError_code() + ":" + result.getError_messsage());
//                        }
//                    }
//                } else {//sell，有多就平多，如果还没吃完这个单，就做空
//                    //freeGoods=权益(或者余额)*持仓率-空仓仓位
//                    qty = Integer.parseInt(position.getString("long_qty"));
//                    if (qty == 0) {//无多，有空
//                        o.setType("2");//开空
//                    } else {//有多，就平多。如果多不够用，也平多,剩余的开空
//                        o.setType("3");//平多
//                        //需要做空的量=订单量-多仓量
//                        double downQty = contractCount - qty;
//
//                        if (downQty > 0) {//如果有必要做空
//                            o.setSize("" + qty);
//                            log.info("多仓不够用，需要做空");
//                            Order oDown = new Order();
//                            //oDown.setPrice(Double.toString(order.getPrice() * (1 + addPrice)));
//                            oDown.setInstrument_id(instrument.getInstrument_id());
//                            oDown.setType("2");
//                            oDown.setSize("" + downQty);
//
//                            oDown.setOrder_type("4");//市价成交
//                            OrderResult result = futuresTradeAPIService.order(oDown);
//                            log.info("空单下单结果" + result.getError_code() + ":" + result.getError_messsage());
//                        }
//                    }
//                }
//            }
//
//            //市价委托。0：普通委托（order type不填或填0都是普通委托）1：只做Maker（Post only）2：全部成交或立即取消（FOK）3：立即成交并取消剩余（IOC）4：市价委托
//            o.setOrder_type("4");
//            //如果合约张数没有设置，才需要设置.说明没有发生大反转.大反转是指【多不够用，也平多,剩余的开空】
//            if (o.getSize() == null) {
//                o.setSize("" + contractCount);//合约张数
//            }
//
//            log.info(platName + "合约张数：" + contractCount);
//
//            OrderResult result = futuresTradeAPIService.order(o);
//            log.info("原单下单结果" + result.getError_code() + ":" + result.getError_messsage());
//
//        }// end for
//
//
//        return userOrderList.size();
//    }
//
//
//    /**
//     * 查出完全成交的订单，并且标记。那么，没被标记的，就是不成功的
//     */
//    @Override
//    public int queryOrderState() throws Exception {
//
//        return 0;
//    }
//
//    /**
//     * 撤销没完全成交的订单
//     *
//     * @throws Exception
//     */
//    @Override
//    public void cancelOrder() throws Exception {
//
//
//    }
//
//    public String getPlatName() {
//        return platName;
//    }
//
//    /**
//     * 提取资产
//     *
//     * @throws Exception
//     */
//    @Override
//    public void withdraw(String productName, double amount, String address) throws Exception {
//        /*List<WithdrawFee> feeResult = this.accountAPIService.getWithdrawFee(productName);
//
//        Withdraw withdraw = new Withdraw();
//        withdraw.setTo_address(address);
//        withdraw.setFee(feeResult.get(0).getMin_fee().toString());
//        withdraw.setCurrency(productName);
//        withdraw.setAmount("" + amount);
//        withdraw.setDestination("4");
//        withdraw.setTrade_pwd("");
//        JSONObject drawResult = this.accountAPIService.withdraw(withdraw);
//        log.info("提币：" + drawResult);
//        */
//    }
//
//    /**
//     * 获取合约属性
//     *
//     * @param underlying 标的指数，如：BTC-USD
//     * @param alias      本周 this_week  次周 next_week  季度 quarter  次季度 bi_quarter
//     * @return
//     */
//    private Instrument getInstrument(String underlying, String alias) {
//        List<Instruments> instrumentList = this.futuresMarketAPIService.getInstruments();
//        for (Instruments obj : instrumentList) {
//            if (obj.getUnderlying().equalsIgnoreCase(underlying) && obj.getAlias().equalsIgnoreCase(alias)) {
//                return new Instrument(obj);
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public double getTotalGoods() {
//        return getAccInfo().getTotalGoods();
//    }
//
//    @Override
//    public double getTotalMoney() {
//        return getAccInfo().getTotalMoney();
//    }
//
//}
//
//

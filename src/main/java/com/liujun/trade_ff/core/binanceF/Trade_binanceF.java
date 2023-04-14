package com.liujun.trade_ff.core.binanceF;

import com.alibaba.fastjson.JSON;
import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Prop;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.binance.api.bean.futures.param.Order;
import com.liujun.trade_ff.core.binance.api.bean.futures.result.*;
import com.liujun.trade_ff.core.binance.api.bean.spot.result.Depth;
import com.liujun.trade_ff.core.binance.api.bean.wallet.param.WithdrawParam;
import com.liujun.trade_ff.core.binance.api.bean.wallet.result.WithdrawResult;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.enums.I18nEnum;
import com.liujun.trade_ff.core.binance.api.enums.OrderSide;
import com.liujun.trade_ff.core.binance.api.enums.OrderType;
import com.liujun.trade_ff.core.binance.api.enums.PositionSide;
import com.liujun.trade_ff.core.binance.api.service.future.FutureAccountAPIService;
import com.liujun.trade_ff.core.binance.api.service.future.FutureOrderAPIService;
import com.liujun.trade_ff.core.binance.api.service.future.FutureProductAPIService;
import com.liujun.trade_ff.core.binance.api.service.future.impl.FutureAccountAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.service.future.impl.FutureOrderAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.service.future.impl.FutureProductAPIServiceImpl;
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
import java.util.List;

 /*

    API接口文档(包含杠杆）：https://binance-docs.github.io/apidocs/future/en/#general-info
期货接口文档：https://binance-docs.github.io/apidocs/futures/cn/#185368440e
API报错自查链接：https://github.com/binance-exchange/binance-official-api-docs/blob/f92d9df35cd926a3514618666ca6ca494c1a734d/errors_CN.md
API交易规则说明：https://binance.zendesk.com/hc/zh-cn/articles/115003235691
API常见问题 (FAQ)：https://binance.zendesk.com/hc/zh-cn/articles/360004492232
     */

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope("prototype")
public class Trade_binanceF extends Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade_binanceF.class);
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
    public static final String platName = "binanceF";


    // ===============================
    private APIConfiguration config;
    private FutureProductAPIService futureProductAPIService;
    private FutureAccountAPIService futureAccountAPIService;
    private FutureOrderAPIService futureOrderAPIService;
    private WalletAPIService walletAPIService;
    private Instrument instrument;
    //多个方法共享变量
    private Account account;
    private Position position;
    private Asset asset;
    private double lastMarkPrice;
    private double longOrShort;
    double long_qty = 0, short_qty = 0;//多、空合约张数
    /**
     * 网址前缀
     */
    @Value("${binanceF.url}")
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
    @Value("${binanceF.feeRate}")
    private double feeRate;
    @Value("${binanceF.contractType}")
    private String contractType;
    @Value("${binanceF.goods}")
    private String goods;
    @Value("${binanceF.money}")
    private String money;
    private String coinPair;
    //------------------------


    public Trade_binanceF(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
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
        this.futureProductAPIService = new FutureProductAPIServiceImpl(this.config);
        this.futureAccountAPIService = new FutureAccountAPIServiceImpl(this.config);
        this.futureOrderAPIService = new FutureOrderAPIServiceImpl(this.config);
        this.walletAPIService = new WalletAPIServiceImpl(this.config);
        coinPair = goods.toUpperCase() + money.toUpperCase();
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
            //https://dapi.binance.com/dapi/v1/depth?symbol=BTCUSD_210625
            Depth depthResult = futureProductAPIService.marketDepth(instrument.getSymbol(), prop.marketOrderSize);
            //同时存储买单和卖单
            List[] localListArr = {depth.getAskList(), depth.getBidList()};
            List[] resultListArr = {depthResult.getAsks(), depthResult.getBids()};
            for (int j = 0; j < 2; j++) {//为了避免重复的代码。就用一个for循环处理买单和卖单
                List<String[]> orderList = resultListArr[j];
                for (int i = 0; i < orderList.size(); i++) {
                    MarketOrder marketOrder = new MarketOrder();// 一个挂单
                    String[] oneOrder = orderList.get(i);
                    marketOrder.setPrice(Double.parseDouble(oneOrder[0]));
                    double contractCount = Double.parseDouble(oneOrder[1]);//合约有多少张
                    double volume = 0;
                    //面额是美元
                    volume = contractCount * instrument.getContractSize() / marketOrder.getPrice();

                    marketOrder.setVolume(volume);
                    marketOrder.setPlatId(platId);

                    localListArr[j].add(marketOrder);
                }//end for i

            }//end for j

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
            this.instrument = getInstrument(contractType, coinPair);
            double contractVal = instrument.getContractSize();
            account = futureAccountAPIService.accountInfo(recvWindow);
            //寻找仓位。系统只开启了单向持仓模式。所以只需要提取positionSide=BOTH的仓位
            //如果是双向持仓模式，同一个合约会显示BOTH/LONG/SHORT三种仓位
            for (int i = 0; i < account.getPositions().size(); i++) {
                position = account.getPositions().get(i);
                if (instrument.getSymbol().equals(position.getSymbol())
                        && PositionSide.BOTH.name().equals(position.getPositionSide())) {
                    break;
                }
            }
            for (int i = 0; i < account.getAssets().size(); i++) {
                asset = account.getAssets().get(i);
                if (asset.getAsset().equals(instrument.getMarginAsset())) {
                    break;
                }
            }
            //判断合约张数，合约方向(做多还是做空)。平均持仓成本和最新标记价格配合，能知道自己持有的是多单还是空单。
            lastMarkPrice = futureProductAPIService.lastMarkPrice(instrument.getSymbol());
            longOrShort = findSide(lastMarkPrice, position);

            //获取最新价格。如果获取不到，就用最新标记价格替代
            double lastPrice = getCurrentPrice() == 1.0 ? lastMarkPrice : getCurrentPrice();

            AccountInfo accountInfo = new AccountInfo();
            double equity = asset.getMarginBalance();//权益
            //如果是币本位合约，余额是货
            if (instrument.getMarginAsset().equalsIgnoreCase(goods)) {
                //freeMoney=权益(或者余额)*持仓率-多仓仓位,   freeGoods=权益(或者余额)*持仓率-空仓仓位
                //freezedMoney，根据持仓量表示的一个参数。用来检测平衡：确保钱恒定。做空，产生钱，消耗货。做多，消耗钱，产生货
                //因为面额是美元
                if (longOrShort > 0) {//所需保证金*标记价格/合约面额
                    long_qty = position.getInitialMargin() * lastMarkPrice / contractVal;
                    accountInfo.setFreeMoney((equity * prop.positionRate - position.getInitialMargin()) * lastMarkPrice);
                    accountInfo.setFreeGoods(equity * prop.positionRate - 0);
                    accountInfo.setFreezedMoney(-1 * position.getInitialMargin() * lastMarkPrice);
                    accountInfo.setFreezedGoods(1 * position.getInitialMargin());
                } else if (longOrShort < 0) {
                    short_qty = position.getInitialMargin() * lastMarkPrice / contractVal;
                    accountInfo.setFreeMoney((equity * prop.positionRate - 0) * lastMarkPrice);
                    accountInfo.setFreeGoods(equity * prop.positionRate - position.getInitialMargin());
                    accountInfo.setFreezedMoney(1 * position.getInitialMargin() * lastMarkPrice);
                    accountInfo.setFreezedGoods(-1 * position.getInitialMargin());
                }

                accountInfo.setTotalMoney(accountInfo.getFreezedMoney());
                accountInfo.setTotalGoods(equity);
            }

            //如果是usdt本位合约,余额是钱
            if (instrument.getMarginAsset().equalsIgnoreCase(money)) {
                //因为面额是美元
                if (longOrShort > 0) {//所需保证金*标记价格/合约面额
                    long_qty = position.getInitialMargin() / contractVal;
                    accountInfo.setFreeMoney(equity * prop.positionRate - position.getInitialMargin());
                    accountInfo.setFreeGoods((equity * prop.positionRate - 0) / lastMarkPrice);
                    accountInfo.setFreezedMoney(-1 * position.getInitialMargin());
                    accountInfo.setFreezedGoods(1 * position.getInitialMargin() / lastMarkPrice);
                } else if (longOrShort < 0) {
                    short_qty = position.getInitialMargin() / contractVal;
                    accountInfo.setFreeMoney(equity * prop.positionRate - 0);
                    accountInfo.setFreeGoods((equity * prop.positionRate - position.getInitialMargin()) / lastMarkPrice);
                    accountInfo.setFreezedMoney(1 * position.getInitialMargin());
                    accountInfo.setFreezedGoods(-1 * position.getInitialMargin() / lastMarkPrice);
                }


                accountInfo.setTotalMoney(equity);
                accountInfo.setTotalGoods(accountInfo.getFreezedGoods());
            }

            //设置仓位状态。空仓还是持仓
            if (longOrShort == 0) {
                engine.setFutureState(Engine.FUTURE_EMPTY);
            } else {
                engine.setFutureState(Engine.FUTURE_HOLD);
            }


            setAccInfo(accountInfo);

        } catch (Exception e) {
            log.error(getPlatName() + " : " + e.getMessage(), e);
            throw e;
        }

    }

    /**
     * 判断合约方向(做多还是做空)。平均持仓成本和最新标记价格配合，能知道自己持有的是多单还是空单。
     * 如果价格走势和自己盈亏走势一致，那就是多单。
     *
     * @param lastMarkPrice 最新标记价格
     * @param position      仓位
     * @return true:多单 false空单
     */
    private double findSide(double lastMarkPrice, Position position) {
        return (lastMarkPrice - position.getEntryPrice()) * position.getUnrealizedProfit();
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

            double contractCount = 0;//需要执行的订单，包含多少张合约
            //面额是美元
            contractCount = order.getVolume() * order.getPrice() / instrument.getContractSize();


            // 为了确保能成交，可以将卖单价格降低。买单不能动。因为可能导致money不够。
            double addPrice = 0;// (order.getType().equals("sell") ? -1 * prop.huaDian2 : prop.huaDian2);
            Order o = new Order();
            o.setPrice(order.getPrice() * (1 + addPrice));
            o.setSymbol(instrument.getSymbol());
            //如果无仓，说明要开仓
            log.info("挂单前的状态：engine.getFutureState():" + engine.getFutureState());
            if (engine.getFutureState().equals(Engine.FUTURE_EMPTY)) {
                if (order.getType().equals("buy")) {
                    o.setSide(OrderSide.BUY);//开多
                } else {
                    o.setSide(OrderSide.SELL);//开空
                }
            } else {//如果有仓位,说明要平仓，还是加仓？
                //查出现有仓位，看当前是在做多还是做空。
                if (order.getType().equals("buy")) {//有空就平空，如果还没吃完这个单，就做多。还有能力继续做多吗？肯定有：有多仓仓位，又有买单没过滤掉，说明多仓没满
                    //freeMoney=权益(或者余额)*持仓率-多仓仓位
                    if (short_qty == 0) {//无空,有多
                        o.setSide(OrderSide.BUY);//开多
                    } else {//有空，就平空。如果空不够用，也平空,剩余的开多
                        o.setSide(OrderSide.BUY);//平空
                        //需要做多的量=订单量-空仓量
                        int upQty = (int) Math.round(contractCount - short_qty);

                        if (upQty > 0) {//如果有必要做多
                            o.setClosePosition("true");
                            log.info("空仓不够用，需要做多");
                            Order oUp = new Order();
                            oUp.setPrice(order.getPrice() * (1 + addPrice));
                            oUp.setSymbol(instrument.getSymbol());
                            oUp.setSide(OrderSide.BUY);
                            oUp.setQuantity(upQty);
                            oUp.setType(OrderType.MARKET);//市价成交
                            OrderResult result = futureOrderAPIService.addOrder(oUp);
                            log.info("多单下单结果" + JSON.toJSONString(result));
                        }
                    }
                } else {//sell，有多就平多，如果还没吃完这个单，就做空
                    //freeGoods=权益(或者余额)*持仓率-空仓仓位
                    if (long_qty == 0) {//无多，有空
                        o.setSide(OrderSide.SELL);//开空
                    } else {//有多，就平多。如果多不够用，也平多,剩余的开空
                        o.setSide(OrderSide.SELL);//平多
                        //需要做空的量=订单量-多仓量
                        int downQty = (int) Math.round(contractCount - long_qty);

                        if (downQty > 0) {//如果有必要做空
                            o.setClosePosition("true");
                            log.info("多仓不够用，需要做空");
                            Order oDown = new Order();
                            oDown.setPrice(order.getPrice() * (1 + addPrice));
                            oDown.setSymbol(instrument.getSymbol());
                            oDown.setSide(OrderSide.SELL);
                            oDown.setQuantity(downQty);

                            oDown.setType(OrderType.MARKET);//市价成交
                            OrderResult result = futureOrderAPIService.addOrder(oDown);
                            log.info("空单下单结果" + JSON.toJSONString(result));
                        }
                    }
                }
            }

            //市价委托。0：普通委托（order type不填或填0都是普通委托）1：只做Maker（Post only）2：全部成交或立即取消（FOK）3：立即成交并取消剩余（IOC）4：市价委托
            o.setType(OrderType.MARKET);
            //如果合约张数没有设置，才需要设置.说明没有发生大反转.大反转是指【多不够用，也平多,剩余的开空】
            if (o.getQuantity() == 0) {
                o.setQuantity((int) Math.round(contractCount));//合约张数
            }

            log.info(platName + "合约张数：" + contractCount);

            OrderResult result = futureOrderAPIService.addOrder(o);
            log.info("原单下单结果" + JSON.toJSONString(result));


        }// end for


        return userOrderList.size();
    }


    /**
     * 查出完全成交的订单，并且标记。那么，没被标记的，就是不成功的
     */
    @Override
    public int queryOrderState() throws Exception {
        return 0;
    }

    /**
     * 撤销没完全成交的订单
     *
     * @throws Exception
     */
    @Override
    public void cancelOrder() throws Exception {


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

    /**
     * 获取合约属性
     *
     * @param contractType 合约类型：本季：CURRENT_QUARTER， 下季：NEXT_QUARTER， 永续合约：PERPETUAL
     * @param pair         BTCUSD
     * @return
     */
    private Instrument getInstrument(String contractType, String pair) {
        List<Instrument> instrumentList = this.futureProductAPIService.getInstruments();
        for (Instrument obj : instrumentList) {
            if (obj.getContractType().equals(contractType) && obj.getPair().equals(pair)) {
                return obj;
            }
        }
        return null;
    }

    @Override
    public double getTotalGoods() {
        return getAccInfo().getTotalGoods();
    }

    @Override
    public double getTotalMoney() {
        return getAccInfo().getTotalMoney();
    }

}

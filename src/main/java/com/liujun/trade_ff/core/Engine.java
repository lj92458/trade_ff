package com.liujun.trade_ff.core;

import com.liujun.trade_ff.core.modle.*;
import com.liujun.trade_ff.core.thread.AvgpriceThread;
import com.liujun.trade_ff.core.util.HttpUtil;
import com.liujun.trade_ff.core.util.StringUtil;
import com.liujun.trade_ff.core.util.TransTokenUtil;
import com.liujun.trade_ff.core.util.XmlConfigUtil;
import com.liujun.trade_ff.utils.SpringContextUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 搬砖引擎,程序入口。负责总的调度。
 *
 * @author Administrator
 */
@Component
@Scope("prototype")
public class Engine {
    // ------- static 属性 ---------
    private static final Logger log = LoggerFactory.getLogger(Engine.class);
    //值得搬运
    private static final Logger log_needTrade = LoggerFactory.getLogger("need_trade");
    //已成交
    private static final Logger log_haveTrade = LoggerFactory.getLogger("have_trade");
    //差价
    private static final Logger log_diff_price = LoggerFactory.getLogger("diff_price");

    private static final String charset = "utf-8";
    private static final double FIRST_FAIL = -99999998;
    private static final double SECOND_FAIL = -99999999;
    private static final String CHANGE_PRICE = "changePrice";
    public static PriceInfo priceInfo;
    public static final String FUTURE_EMPTY = "empty";
    public static final String FUTURE_HOLD = "hold";
    // ---- end static ------------------------------------------------

    // --------- 对象属性 -------------------------------------------------
    @Autowired
    Prop prop;
    public boolean initSuccess = false;//初始化是否成功
    public boolean stop = false;//是否结束
    public String firstBalance;
    public Document xmlDoc;// 可修改可存储的配置参数,xml文件。在jar文件外面
    public AvgpriceThread avgpriceThread;
    public int maxOrderNum = 75;//最多处理多少市场挂单

    /**
     * 间隔多久查询一次挂单.单位：秒
     */
    @Value("${engine.time_queryOrder}")
    public int time_queryOrder;
    /**
     * 每循环一次,最大允许占用的时间.单位：秒
     */
    @Value("${engine.time_oneCycle}")
    public int time_oneCycle;

    /**
     * 如果抛出异常,暂停多少分钟？
     */
    @Value("${engine.waitSecondAfterException}")
    public int waitSecondAfterException;

    /**
     * 每天几点开始记录余额
     */
    @Value("${engine.time_beginBalance}")
    public int time_beginBalance;
    /**
     * 间隔多久,计算余额并记录日志
     */
    @Value("${engine.time_waitBalance}")
    public int time_waitBalance;
    /**
     * 余额文件的路径
     */
    @Value("${balanceFilePath}")
    public String balanceFilePath;

    /**
     * 订单匹配模式.simple简单匹配, exact精细匹配
     */
    @Value("${engine.trade_model}")
    public String tradeModel;
    @Value("${trade.core.package}")
    public String corePackage;
    /**
     * #dex和cex同步挂单吗？true同步，false不同步。如果dex失败率高，就不要同步挂单。而是先让dex执行，执行成功后会发现资金失衡，然后通过调平资金的方式去执行cex
     */
    @Value("${trade.dexSync}")
    public boolean dexSync;
    /**
     * 任意平台的token比例降到多少，就触发转账
     */
    @Value("${trade.whenBalance}")
    public double whenBalance;
    @Value("${trade.canBalance}")
    public boolean canBalance;
    @Value("${trade.needCheckTotalAmount}")
    public boolean needCheckTotalAmount;
    // ====重要属性=============
    /**
     * 存放各个平台的交易对象
     */
    public List<Trade> platList;
    /**
     * 存放价格限制属性名:A_B
     */
    public String[] keyArray;
    /**
     * xml文件中存放各平台价格限制
     */
    public double[] priceArray;

    public VirtualTrade virtualTrade;// 虚拟的平台

    private String futureState;//期现套利状态：empty空仓，hold持仓

    private double openPriceGap;//期货开仓时，两个平台之间的差价.跟配置文件中平台出现的先后顺序有关：用前一个平台的价格减后一个平台

    private double goodsRate;//goods价值占总投资额的比例
    public String xmlFilePath = "./conf.xml";

    private java.util.concurrent.ThreadPoolExecutor threadPoolExecutor;
    // ====================


    /**
     * 最后一次记录的余额
     */
    public Balance lastBalance;
    public HttpUtil httpUtil = new HttpUtil();
    public Balance currentBalance;


    public ChangeLimit changeLimit;
    /**
     * 美元对人民币汇率。这里是用比特币给山寨币计价，不存在汇率，所以设为1
     */
    public double usdRate = 1.0;
    /**
     * 资金调平，是否已完成、已到账？
     */
    public boolean isBalanceFinished = true;

    // --------- end 对象属性 -------------------------------------------------
    static {
        log.info("in Engine static 代码块");
    }

    @PostConstruct
    public void init() {
        try {
            log.info("当前路径：" + new File("./").getAbsolutePath());
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(Paths.get(xmlFilePath)), charset)) {
                SAXReader sax = new SAXReader();
                xmlDoc = sax.read(reader);

            } catch (Exception e) {
                log.error("engine配置文件加载异常conf.xml", e);
                throw e;
            }

            firstBalance = readXmlProp("firstBalance");

            openPriceGap = Double.parseDouble(readXmlProp("openPriceGap"));

            goodsRate = Double.parseDouble(readXmlProp("goodsRate"));
            if (goodsRate < 0 || goodsRate > 1) {
                throw new Exception("goodsRate的取值范围是[0,1]");
            }
            //---------------

            // 存放各个平台的交易对象
            platList = new ArrayList<Trade>();
            String[] paltNameArr = readXmlProp("enablePlat").split(",");
            for (String platName : paltNameArr) {//利用反射，加载各平台的对象
                Class<Trade> clazz = (Class<Trade>) Class.forName(corePackage + "." + platName + ".Trade_" + platName, true,
                        getClass().getClassLoader());
                Trade trade = SpringContextUtil.getBean(clazz, httpUtil, platList.size(), usdRate, prop, this);
                //如果初始化失败了
                if (!trade.initSuccess) {
                    throw new Exception(platName + ":初始化失败!!!");
                }
                //设置配置属性
                trade.setChangePrice(Double.parseDouble(readXmlAttribute(platName, CHANGE_PRICE)));
                trade.pToken = new double[]{
                        Double.parseDouble(readXmlAttribute(platName, "pgoods")),
                        Double.parseDouble(readXmlAttribute(platName, "pmoney"))
                };

                platList.add(trade);
            }
            //检查合法性
            double sumPrice = platList.stream().mapToDouble(Trade::getChangePrice).sum();
            double sumPgoods = platList.stream().mapToDouble(o -> o.pToken[0]).sum();
            double sumPmoney = platList.stream().mapToDouble(o -> o.pToken[1]).sum();
            if (sumPrice != 0 || sumPgoods != 1 || sumPmoney != 1) {
                throw new Exception("price或pgoods或pmoney总和不合法！");
            }

            //添加虚拟平台
            virtualTrade = SpringContextUtil.getBean(VirtualTrade.class, httpUtil, platList.size(), usdRate, prop, this);
            platList.add(virtualTrade);

            //创建threadPoolExecutor
            threadPoolExecutor = new ThreadPoolExecutor(
                    3 * platList.size() + 10,
                    3 * platList.size() + 10,
                    5L, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>(10),
                    new ThreadPoolExecutor.AbortPolicy());

            // 长度是 (n-1)*11+1
            keyArray = new String[(platList.size() - 1) * 11 + 1];
            priceArray = new double[(platList.size() - 1) * 11 + 1];
            for (int i = 0; i < platList.size(); i++) {
                // Trade trade = platList.get(i);
                for (int j = 0; j < platList.size(); j++) {
                    keyArray[i * 10 + j] = platList.get(i).getPlatName() + "_" + platList.get(j).getPlatName();
                    String priceStr = readXmlProp(keyArray[i * 10 + j]);
                    if (i == j || StringUtil.isEmpty(priceStr)) {
                        priceArray[i * 10 + j] = 0;
                    } else {
                        priceArray[i * 10 + j] = Double.parseDouble(priceStr);
                    }
                }// end for

            }// end for
            if (priceInfo == null || priceInfo.platCount != platList.size()) {//只创建一次
                priceInfo = new PriceInfo(platList.size());
            }
            changeLimit = SpringContextUtil.getBean(ChangeLimit.class);
            changeLimit.setEngine(this);
            // ------设置余额记录--------------------------
            File balanceFile = new File(balanceFilePath);

            // 如果余额文件存在,就读取最后一行,赋值给lastBalance
            try (ReversedLinesFileReader reversedLinesReader = new ReversedLinesFileReader(balanceFile, Charset.forName(charset))) {
                String lastBalanceStr = reversedLinesReader.readLine();
                reversedLinesReader.close();
                lastBalance = new Balance(prop, lastBalanceStr);
            } catch (Exception e) {
                // 如果余额文件不存在,就创建并写入初始记录,并赋值给lastBalance
                FileUtils.writeStringToFile(balanceFile, firstBalance, charset);
                lastBalance = new Balance(prop, firstBalance);
            }
            currentBalance = getCurrentBalance();
            // end 设置余额记录---------------------------

            //启动差价记录线程. 把AvgpriceThread看作runnable
            avgpriceThread = SpringContextUtil.getBean(AvgpriceThread.class);
            avgpriceThread.setEngine(this);
            CompletableFuture.runAsync(avgpriceThread, threadPoolExecutor);
            log.info("启动平均值线程");

            //
            this.initSuccess = true;
        } catch (Exception e) {
            log.error("初始化 enging出现异常", e);
        }
    }

    /**
     * 启动搬运引擎
     *
     * @return 1表示正常退出,-1表示异常退出
     */
    public int startEngine() {
        stop = false;
        log.info("开始搬运......");
        try {

            for (long i = 0; !stop && i < 86400 / time_queryOrder; i++) {// 每隔24小时(),自动退出
                //
                long beginTime = System.currentTimeMillis();

                balanceAccount(i);//检测是否平衡，如果发现不平，就调平

                if (isBalanceFinished) {
                    //查询市场挂单，以及账户余额
                    queryMarketDepth(i);
                    //匹配/撮合市场挂单。
                    matchMarketDepth();
                    //匹配/撮合备份的市场挂单,并完成交易(已确保我方有相应的资金，能吃掉这些挂单)。这两种匹配是独立的，没有关系。
                    matchBackupDepth();
                    // 盘点当前余额,计算盈亏------------------------
                    saveBalance();
                    //检查系统健康状况
                    checkStatus(beginTime);
                }
                // 睡眠一段时间,保证两次搬运间隔time_queryOrder秒
                long useTime = System.currentTimeMillis() - beginTime;// 用时
                TimeUnit.MILLISECONDS.sleep(time_queryOrder * 1000L - useTime);

            }// end for
            log.info("跳出for,   startEngine()结束");

            return 1;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return -1;
        } finally {

            try {
                httpUtil.getHttpClient().close();
            } catch (IOException e) {
                log.error("HttpClient关闭时出现异常", e);
            }
        }
    }

    /**
     * 每隔一定时间，检测并调整goods总量、平衡两种token、查询余额
     *
     * @param i
     * @throws Exception
     */
    private void balanceAccount(long i) throws Exception {
        if (isBalanceFinished) {//如果资金调平已完成(已到账)，才让引擎正常工作。如果没完成就只能等待
            //查询资金情况 -----------间隔小于6秒时，每隔6秒，查一次账户。否则每次都查.
            if (i != 0 && (time_queryOrder > 6 || (i * time_queryOrder) % 6 == 0)) {
                boolean needBalance = true;
                this.currentBalance = getCurrentBalance();
                try {
                    needBalance = balanceTokens();// 检查各平台的coin数量,如果分布不平衡,就自动转移。转移成功后，再查询账户
                } finally {
                    if (needBalance) {//只有真正的转移了资金，才需要查询账户
                        isBalanceFinished = false;
                    } else if (needCheckTotalAmount) {
                        if ((i * time_queryOrder) % 12 == 0) {
                            flushAccount(true);
                        }
                        // 检查goods总数量,如果不跟初始值相等,就立即买卖调整。为什么要设置在这里呢？因为挂单后，可能导致超时。然后就抛出异常，跳出for循环没机会检查goods
                        //只有当资金分布均匀，才能处理资金总量的变动。因为前者会误导后者
                        if (prop.earnMoney) checkTotalGoods();
                        else checkTotalMoney();

                    }
                }
            }
        } else {//等待资金到账。balanceTokens能确保资金已到账呢
            flushAccount(true);
            //资金调拨，还没到账，这时总资金必然减少。通过监控资金总额，如果减少了，就让整个引擎空转，直到资金回归正常
            Balance bal = getCurrentBalance();
            log.info(bal.getPlatInfo());
            isBalanceFinished = bal.totalToken[0] / currentBalance.totalToken[0] > 0.99
                    && bal.totalToken[1] / currentBalance.totalToken[1] > 0.99;
            if (!isBalanceFinished) {
                log.info("资金没有平衡，系统正在等它平衡......");
            } else if (isBalanceFinished) {
                log.info("资金已经平衡");
            }
        }

    }

    /**
     * 查询余额。如果账户有变动，就要同步查询。否则异步
     *
     * @param isSync 是否需要同步查询
     * @throws Exception
     */
    private void flushAccount(boolean isSync) throws Exception {
        List<CompletableFuture<?>> flushAccountFutureList = new ArrayList<>();
        for (Trade trade : platList) {
            flushAccountFutureList.add(CompletableFuture.runAsync(() -> {
                try {
                    trade.flushAccountInfo();
                } catch (Exception e) {
                    log.error(trade.getPlatName() + "账户查询异常:" + e.getMessage(), e);
                }
            }, threadPoolExecutor));
        }
        if (isSync) {//如果真的做了转账，才需要同步
            CompletableFuture.allOf(flushAccountFutureList.toArray(new CompletableFuture<?>[0])).get(5, TimeUnit.SECONDS);
        }
    }

    /**
     * 查询市场深度和账户余额。并设置到trade.marketDepth属性
     *
     * @throws Exception 异常
     */
    private void queryMarketDepth(long i) throws Exception {
        // ===== 【多线程】对各平台查询市场挂单=======================
        List<CompletableFuture<?>> flushMarketFutureList = new ArrayList<>();

        // 为每个平台启动一个查询线程
        for (Trade trade : platList) {
            // 设置“用户挂单”
            if (trade.getUserOrderList() == null || trade.getUserOrderList().size() > 0) {
                trade.setUserOrderList(new ArrayList<>());
            }
            //如果非同时挂单，就不打算从dex调节goods，那么调节goods时就没必要让dex参与。因为这个过程需要花费好几秒钟，为什么不节省掉呢。【这时还是应该清空dex的userOrderList】
            if (needSkipDexWhenAdjustGoods(trade)) {
                continue;
            }
            flushMarketFutureList.add(CompletableFuture.runAsync(() -> {
                try {
                    trade.flushMarketDeeps();
                } catch (Exception e) {
                    log.error(trade.getPlatName() + "查询市场深度异常:" + e.getMessage(), e);
                }
            }, threadPoolExecutor));

        }//end for
        // 等待各个线程结束,最多等25秒.因为uniswap获取市场行情，需要8秒，重复尝试3次就有24秒-------
        CompletableFuture.allOf(flushMarketFutureList.toArray(new CompletableFuture<?>[0])).get(25, TimeUnit.SECONDS);
        // ==== end【多线程】对各平台查询市场挂单=======================
    }

    /**
     * 撮合市场挂单
     *
     * @throws Exception 异常
     */
    private void matchMarketDepth() throws Exception {
        // 设置综合深度
        ArrayList<MarketOrder>[] totalDepth = new ArrayList[]{new ArrayList<MarketOrder>(), new ArrayList<MarketOrder>()};
        for (Trade trade : platList) {
            //如果非同时挂单，就不打算从dex调节goods，那么调节goods时就没必要让dex参与
            if (needSkipDexWhenAdjustGoods(trade)) {
                continue;
            }
            totalDepth[0].addAll(trade.getMarketDepth()[0]);
            totalDepth[1].addAll(trade.getMarketDepth()[1]);

        }

        totalSort(totalDepth);// 对汇总的市场挂单进行排序：买方从大到小排序,卖方从小到大排序
        log.debug(totalDepth[1] + "");/////////////////
        //调节限价
        EarnCost maxEarnCost = null;
        if (tradeModel.equals("simple")) {
            maxEarnCost = adjustLimitPrice1(totalDepth);
        } else if (tradeModel.equals("exact")) {
            maxEarnCost = adjustLimitPrice2(totalDepth);
        }
        //收益率要大于配置的值
        assert maxEarnCost != null;
        if (maxEarnCost.orderPair > 0 && (//maxEarnCost.earn已经考虑到了矿工费
                (maxEarnCost.earn >= prop.minMoney && maxEarnCost.earn / maxEarnCost.cost >= prop.atLeastRate / 10.0)//atLeastRate不要用来限制市场查询，因此除以10
        )
        ) {// 只有模拟生成的订单存在时，才搬运
            log_needTrade.info("(机会)市场最大差价" + Prop.fmt_money.get().format(maxEarnCost.diffPrice) + prop.money +
                    "，利润率" + prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) + "%," + maxEarnCost.diffPriceDirection
                    + ",最多能赚" + Prop.fmt_money.get().format(maxEarnCost.earn) + prop.money + ",模拟订单有" + maxEarnCost.orderPair + "对");
        } else {
            log.info("最多赚" + maxEarnCost.earn + prop.money + ",模拟订单有" + maxEarnCost.orderPair +
                    "对,利润率" + (maxEarnCost.earn > 0 ? prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) : 0) + "%," +
                    maxEarnCost.diffPriceDirection + "----------------------最大差价" + Prop.fmt_money.get().format(maxEarnCost.diffPrice) + prop.money);
        }
    }

    /**
     * 撮合备份的市场挂单。（根据我方的资金量情况，只把跟资金量匹配的挂单保存起来。确保我方有能力吃掉这些挂单
     */
    private void matchBackupDepth() throws Exception {
        // 将各平台备份的市场挂单导入汇总挂单(因为调节限价时，会把挂单的goods量改为0)
        ArrayList<MarketOrder>[] totalDepth = new ArrayList[]{new ArrayList<MarketOrder>(), new ArrayList<MarketOrder>()};

        for (Trade trade : platList) {
            //如果非同时挂单，就不打算从dex调节goods，那么调节goods时就没必要让dex参与
            if (needSkipDexWhenAdjustGoods(trade)) {
                continue;
            }
            //如果允许跨平台搬运
            if (trade.getModeLock() == 0) {
                trade.setModeLock(1);//加锁
                totalDepth[0].addAll(trade.getBackupDepth()[0]);
                totalDepth[1].addAll(trade.getBackupDepth()[1]);
            }
        }
        totalSort(totalDepth);// 对汇总的市场挂单进行排序：买方从大到小排序,卖方从小到大排序
        log.debug(totalDepth[1] + "");/////////////////
        //如果平台备份的有。
        if (totalDepth[1].size() > 0 && totalDepth[0].size() > 0) {

            double diffPrice = totalDepth[1].get(0).getPrice()
                    - totalDepth[0].get(0).getPrice();
            if (diffPrice > 0) {

                log.debug("市场买单：" + totalDepth[1].toString());
                log.debug("市场卖单：" + totalDepth[0].toString());

                //begin: 根据备份的市场深度求当前市场价格，然后设置给虚拟平台
                double avgPrice = (totalDepth[1].get(0).getPrice() + totalDepth[0].get(0)
                        .getPrice()) / 2.0;
                List<MarketOrder> virtualAskList = virtualTrade.getBackupDepth()[0];
                if (virtualAskList.size() > 0) {//降低市场卖单价格，确保真实平台能卖出
                    virtualAskList.get(0).setPrice(avgPrice * (1 - prop.huaDian));
                }
                List<MarketOrder> virtualBidList = virtualTrade.getBackupDepth()[1];
                if (virtualBidList.size() > 0) {//提高市场买单价格，确保真实平台能买到
                    virtualBidList.get(0).setPrice(avgPrice * (1 + prop.huaDian));
                }
                // end : 根据备份的市场深度求当前市场价格，然后设置给虚拟平台

                int keyIndex = totalDepth[1].get(0).getPlatId() * 10
                        + totalDepth[0].get(0).getPlatId();
                log.info("可搬运最大差价【" + diffPrice + "】" + keyArray[keyIndex] + " " + totalDepth[1].get(0)
                        + "," + totalDepth[0].get(0)); //


                EarnCost maxEarnCost = null;
                if (tradeModel.equals("simple")) {
                    maxEarnCost = createOrders1(totalDepth);// 正式生成订单
                } else if (tradeModel.equals("exact")) {
                    maxEarnCost = createOrders2(totalDepth);
                }

                //收益率要大于0.4%
                assert maxEarnCost != null;
                if (maxEarnCost.orderPair > 0 && (//maxEarnCost.earn已经考虑到了矿工费
                        (maxEarnCost.earn >= prop.minMoney && maxEarnCost.earn / maxEarnCost.cost >= prop.atLeastRate)
                                || virtualTrade.isActive())
                ) {// (正式生成的订单数量)
                    log_needTrade.info("实际能赚" + maxEarnCost.earn + prop.money + "，利润率" + prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) + "%，实际订单有" + maxEarnCost.orderPair + "对");

                    //检查各平台的收益率是否合规，如果全部合规，才能启动交易
                    boolean profitRateMatch = true;
                    for (Trade trade : platList) {

                        if (trade.getUserOrderList().size() > 0) {
                            trade.profitRate = trade.profitRate();
                            if (!virtualTrade.isActive() && trade.profitRate < prop.atLeastRate) {
                                profitRateMatch = false;
                                log.error(trade.getPlatName() + "当前收益率" + trade.profitRate + "小于规定的收益率" + prop.atLeastRate);
                            }
                        }
                    }

                    //如果各平台收益率都合规，才应该启动线程
                    if (profitRateMatch) {
                        // 【多线程】对各平台执行挂单、查订单状态、撤销没完全成交的订单、刷新账户信息==================
                        /*1.先执行dex平台，如果成功，再执行cex平台？？？这样好吗？
                         不好：dex平台浪费了一分钟时间，这时cex平台的情况已经变动了。cex生成的订单不应该被提交，应该直接作废。
                         更好的办法是：在下一轮循环时会调节goods数量(只用cex来调节)，这样间接的执行了cex平台.
                         2.只有期货平台不需要自动调节goods，所以期货和现货，代码还是要分开的。
                         3.dex订单一提交，系统就会阻塞，直到交易被打包。如果只有dex交易成功:
                            a.如果dex消耗的是goods，系统会报money增多.平衡模块会自动买goods(如果dexSync=true,两平台同时挂单，那么就应该从两个平台调节goods)
                            b.如果dex消耗的是money, 会导致goods增多，平衡模块会自动卖goods
                        */
                        executeTrade();

                        // end 【多线程】对各平台执行挂单、查订单状态、撤销没完全成交的订单、刷新账户信息============
                    }
                } else {//end 如果正式生成的订单数量>0
                    log.info("正式订单最多赚" + maxEarnCost.earn + prop.money + ",利润率" + (maxEarnCost.earn > 0 ? prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) : 0) + "%," + maxEarnCost.orderPair + "对订单(不值得/看不上)----------------------");

                }
            }
        } else {//如果平台没有备份挂单,说明平台上没有资金
            if (totalDepth[1].size() == 0) {
                log.info("平台资金" + prop.goods + "已耗尽----------------------");
            }
            if (totalDepth[0].size() == 0) {
                log.info("平台资金" + prop.money + "已耗尽----------------------");
            }

        }
        //解锁
        for (Trade trade : platList) {
            if (trade.getModeLock() == 1) {
                trade.setModeLock(0);
            }
        }

    }

    private void checkStatus(long beginTime) throws Exception {
        // 计算耗时,如果大于最大限度,就报错
        long useTime = System.currentTimeMillis() - beginTime;// 用时
        log.info("totalMoney: " + currentBalance.totalToken[1] + ", totalGoods: " + currentBalance.totalToken[0] + ", {" + currentBalance.getPlatInfo() + "}");
        if (useTime > 5 * 60 * 1000) {// 如果用时大于5分钟
            throw new Exception("本次超时！耗时" + (useTime / 1000.0) + "秒++++++++++++++++++++++++++++++++++++++");
        } else if (useTime > (time_oneCycle * 1000L)) {
            log.warn("xxxxxxxxxxxxxx本次超时！耗时" + (useTime / 1000.0) + "秒xxxxxxxxxxxxxxxtotalEarn:"
                    + currentBalance.getTotalEarn() + prop.earnWhat + "===thisEarn:" + currentBalance.getThisEarn()
                    + prop.earnWhat + " xxxxx");
        } else {
            log.info("==============本次耗时" + useTime + "毫秒=======totalEarn:" + currentBalance.getTotalEarn()
                    + prop.earnWhat + "===thisEarn:" + currentBalance.getThisEarn() + prop.earnWhat + "============");
        }
        //检测亏损
        if (currentBalance.getThisEarn() <= -8.0 / prop.moneyPrice) {
            //throw new Exception("出现亏损，暂停搬运。");
        }
    }

    /**
     * 对各交易平台，进行挂单操作
     *
     * @throws Exception 异常
     */
    private void executeTrade() throws Exception {
        List<CompletableFuture<?>> tradeFutureList = new ArrayList<>();
        // 为每个平台启动一个线程--------
        //计算总的固定费用，如果>0,说明有dex平台参与，那么由dexSync参数决定是否执行cex
        double totalFixFee = platList.stream().filter(trade -> trade.getUserOrderList().size() > 0).mapToDouble(Trade::getFixFee).sum();
        try {
            for (Trade trade : platList) {
                if (trade.getUserOrderList().size() == 0)
                    continue;
            /*如果有dex平台存在，跳过cex平台，只执行dex，如果dex执行成功，在下个循环通过调节goods数量，间接执行了cex。
             这样作的好处是：dex踏空率太高了，一旦dex踏空，cex也就没必要执行了，多省事啊。
             */
                if (!dexSync && totalFixFee > 0 && trade.getFixFee() == 0) {
                    continue;
                }

                tradeFutureList.add(CompletableFuture.runAsync(() -> {
                    long beginTime = System.currentTimeMillis();
                    try {
                        // 挂单,并返回挂单数量,
                        int orderNum = trade.tradeOrder();
                        if (orderNum > 0) {// 如果挂单数量不为0
                            log_haveTrade.info(trade.getPlatName() + "已挂单" + orderNum + "个：" + trade.getUserOrderList().toString());
                            // 查询订单状态，最多4秒
                            for (int i = 0; i < 4000 / prop.time_sleep; i++) {
                                TimeUnit.MILLISECONDS.sleep(prop.time_sleep);// 睡眠
                                int unFinishedNum = trade.queryOrderState();
                                if (unFinishedNum == 0) {
                                    break;
                                }
                            }//end for
                            trade.cancelOrder();// 撤销没完全成交的订单
                            trade.flushAccountInfo();// 并刷新账户信息
                        } else {
                            log_haveTrade.info(trade.getPlatName() + "--------  0 个挂单---------------------------------------");
                        }
                    } catch (Exception e) {
                        log_haveTrade.error(trade.getPlatName() + "交易异常:" + e.getMessage(), e);
                    }
                    // 计算耗时
                    long endTime = System.currentTimeMillis();
                    log.info("线程结束,耗时" + (endTime - beginTime) + "毫秒*********************");
                }, threadPoolExecutor));

            }// end for
            // 等待各个线程结束,最多等time_oneCycle秒-------
            CompletableFuture.allOf(tradeFutureList.toArray(new CompletableFuture<?>[0])).get(time_oneCycle, TimeUnit.SECONDS);
        } finally {
            flushAccount(true);
        }

        log_haveTrade.info("===================================================================================");
        log.info("各线程都已结束=========");
    }

    /**
     * 对汇总的市场挂单进行排序：买方从大到小排序,卖方从小到大排序
     */
    private void totalSort(ArrayList<MarketOrder>[] totalDepth) {
        // 卖
        Collections.sort(totalDepth[0]);
        // 对买方排序,然后颠倒
        Collections.sort(totalDepth[1]);
        Collections.reverse(totalDepth[1]);
        //log.info("ask挂单量"+totalDepth[0].size()+",bid挂单量"+totalDepth[1].size());
    }

    /**
     * 简单型：任何平台差价达不到阈值，就全部停止匹配
     *
     * @return EarnCost
     * @throws Exception 异常
     */
    private EarnCost adjustLimitPrice1(ArrayList<MarketOrder>[] totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth[0];
        List<MarketOrder> bidList = totalDepth[1];
        // 早已不满足条件？
        boolean[] passArr = new boolean[(platList.size() - 1) * 11 + 1];//
        // adjust1是否已处理过
        boolean[] passAdjust1Arr = new boolean[(platList.size() - 1) * 11 + 1];//
        Set<Integer> platIdSet = new HashSet<>();
        while (askList.size() > 0 && bidList.size() > 0) {
            if (askList.get(0).getVolume() < prop.minAmount) {
                askList.remove(0);
            }
            if (bidList.get(0).getVolume() < prop.minAmount) {
                bidList.remove(0);
            }
            if (askList.size() == 0 || bidList.size() == 0) {
                break;
            }
            EarnCost thisEarnCost = helpadjustLimit(askList.get(0), bidList.get(0), passArr, passAdjust1Arr, platIdSet, maxEarnCost);
            if (thisEarnCost.earn > FIRST_FAIL) {// 如果满足条件，才累计金额
                maxEarnCost.earn += thisEarnCost.earn;
                maxEarnCost.cost += thisEarnCost.cost;
            } else {
                //如果差价达不到阈值，就不会扣减双方的volume.导致该挂单没机会被删除。 就必须跳出while循环，否则就是死循环。
                break;
            }
        }//end while

        //如果市场订单被全部用掉，说明获取的订单太少
        if (askList.size() == 0 || bidList.size() == 0) {
            //log.warn("市场深度不足（获取的挂单太少）");
        }
        //maxMoney需要减去平台固定费用(矿工费)
        platList.stream().filter(trade -> platIdSet.contains(trade.platId)).forEach(trade -> maxEarnCost.earn -= trade.getFixFee());
        return maxEarnCost;
    }

    /**
     * 调整阀值。
     *
     * @param passArr        某平台是否早已不满足条件，应该跳过
     * @param passAdjust1Arr 某平台是否应该跳过adjust1的处理
     * @return EarnCost
     * @throws Exception 异常
     */
    private EarnCost helpadjustLimit(MarketOrder ask, MarketOrder bid, boolean[] passArr, boolean[] passAdjust1Arr, Set<Integer> platIdSet, EarnCost maxEarnCost) throws
            Exception {
        int arrayIndex = bid.getPlatId() * 10 + ask.getPlatId();
        // 计算差价
        double diffPrice = bid.getPrice() - ask.getPrice();
        if (diffPrice > maxEarnCost.diffPrice) {
            maxEarnCost.diffPrice = diffPrice;
            maxEarnCost.diffPriceDirection = keyArray[arrayIndex] + ": " + bid.getPrice() + "_" + ask.getPrice();
        }
        double amount = Math.min(ask.getVolume(), bid.getVolume());
        // 如果不是虚拟平台，就调节限价
        if (bid.getPlatId() != ask.getPlatId() && bid.getPlatId() != virtualTrade.platId
                && ask.getPlatId() != virtualTrade.platId) {
            changeLimit.adjust1(diffPrice, amount, arrayIndex, ask, bid, passAdjust1Arr);//
            changeLimit.adjust2(diffPrice, amount, arrayIndex, ask, bid);
            changeLimit.adjust3(diffPrice, amount, arrayIndex, ask, bid);
        }
        // 如果有差价,并且差价大于min_diffPrice,就值得搬运
        if (diffPrice >= priceArray[arrayIndex]) {
            // 寻找两者之中较小的挂单量
            if (amount < prop.minAmount) {// 如果数量太小。（在简单匹配模式，数量不可能太小。太小的已经被删除了。）
                return new EarnCost(0.00, 0);
            }

            maxEarnCost.orderPair++;
            log_diff_price.info("有差价:" + Prop.fmt_money.get().format(diffPrice) + ",数量:" + Prop.fmt_goods.get().format(amount) + ",方向:"
                    + keyArray[arrayIndex] + "," + bid.getPrice() + "_" + ask.getPrice() + "");

            // =======从卖方挂单扣除amount,===========
            ask.setVolume(ask.getVolume() - amount);
            // =====从买方挂单扣除amount,==========
            bid.setVolume(bid.getVolume() - amount);
            // ======设置配对订单============================
            //把平台id记下来
            platIdSet.add(ask.getPlatId());
            platIdSet.add(bid.getPlatId());
            return new EarnCost(diffPrice * amount, (bid.getPrice() + ask.getPrice()) / 2 * amount);
        } else {// 如果不满足条件
            if (passArr[arrayIndex]) {// 如果早已不满足条件了
                return new EarnCost(SECOND_FAIL, 0);
            } else {// 如果是首次不满足条件
                passArr[arrayIndex] = true;
                return new EarnCost(FIRST_FAIL, 0);
            }
        }
    }

    /**
     * 寻找差价,生成订单。简单型：任何平台差价达不到阈值，就全部停止匹配
     *
     * @throws Exception 异常
     */
    private EarnCost createOrders1(ArrayList<MarketOrder>[] totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth[0];
        List<MarketOrder> bidList = totalDepth[1];

        boolean[] passArr = new boolean[(platList.size() - 1) * 11 + 1];// 是否需要搬运
        Set<Integer> platIdSet = new HashSet<>();
        while (askList.size() > 0 && bidList.size() > 0) {
            if (askList.get(0).getVolume() < prop.minAmount) {
                askList.remove(0);
            }
            if (bidList.get(0).getVolume() < prop.minAmount) {
                bidList.remove(0);
            }
            if (askList.size() == 0 || bidList.size() == 0) {
                break;
            }
            EarnCost thisEarnCost = helpCreateOrders(askList.get(0), bidList.get(0), passArr, platIdSet, maxEarnCost);
            if (thisEarnCost.earn > FIRST_FAIL) {// 如果差价达到限制条件，才累计金额（thisEarnCost.earn不一定是正数）
                maxEarnCost.earn += thisEarnCost.earn;
                maxEarnCost.cost += thisEarnCost.cost;

            } else {
                //如果差价达不到阈值，就不会扣减双方的volume.导致该挂单没机会被删除。 就必须跳出while循环，否则就是死循环。
                break;
            }
        }

        //如果市场订单被全部用掉，说明获取的订单太少
        if (askList.size() == 0 || bidList.size() == 0) {
            log.warn("市场深度不足（获取的挂单太少）");
        }
        //maxEarnCost.earn需要减去平台固定费用(矿工费)
        platList.stream().filter(trade -> platIdSet.contains(trade.platId)).forEach(trade -> maxEarnCost.earn -= trade.getFixFee());
        return maxEarnCost;
    }

    /**
     * 返回预计赚的钱。如果是FIRST_FAIL，表示首次出现不满足的情况，如果是SECOND_FAIL表示多次出现不满足的情况
     *
     * @param passArr 不满足条件吗? true表示“不满足”，false表示满足
     * @return EarnCost
     * @throws Exception 异常
     */
    private EarnCost helpCreateOrders(MarketOrder ask, MarketOrder bid, boolean[] passArr, Set<Integer> platIdSet, EarnCost maxEarnCost) throws Exception {

        int arrayIndex = bid.getPlatId() * 10 + ask.getPlatId();
        // 计算差价
        double diffPrice = bid.getPrice() - ask.getPrice();
        double amount = Math.min(ask.getVolume(), bid.getVolume());
        // 如果有差价,并且差价大于min_diffPrice,就值得搬运
        if (diffPrice >= priceArray[arrayIndex]) {
            // 寻找两者之中较小的挂单量
            if (amount < prop.minAmount) {// 如果数量太小。（在简单匹配模式，数量不可能太小。太小的已经被删除了。）
                return new EarnCost(0.00, 0);
            }

            maxEarnCost.orderPair++;
            log_diff_price.info("(实际订单)有差价:" + Prop.fmt_money.get().format(diffPrice) + ",数量:" + Prop.fmt_goods.get().format(amount) + ",方向:"
                    + keyArray[arrayIndex] + "," + bid.getPrice() + "_" + ask.getPrice() + "");

            // =======从卖方挂单扣除amount,并生成买单===========
            ask.setVolume(ask.getVolume() - amount);
            UserOrder order_buy = new UserOrder();
            order_buy.setPlatId(ask.getPlatId());
            order_buy.setType("buy");
            order_buy.setPrice(ask.getPrice());
            order_buy.setDiffPrice(diffPrice);
            order_buy.setVolume(amount);
            Trade trade1 = platList.get(ask.getPlatId());
            trade1.getUserOrderList().add(order_buy);//这里面买单，价格是逐步升高的

            // =====从买方挂单扣除amount,并生成卖单==========
            bid.setVolume(bid.getVolume() - amount);
            UserOrder order_sell = new UserOrder();
            order_sell.setPlatId(bid.getPlatId());
            order_sell.setType("sell");
            order_sell.setPrice(bid.getPrice());
            order_sell.setDiffPrice(diffPrice);
            order_sell.setVolume(amount);
            Trade trade2 = platList.get(bid.getPlatId());
            trade2.getUserOrderList().add(order_sell);//这里面卖单，价格是逐步降低的
            // ======设置配对订单============================
            order_buy.setAnotherOrder(order_sell);
            order_sell.setAnotherOrder(order_buy);
            //把平台id记下来
            platIdSet.add(ask.getPlatId());
            platIdSet.add(bid.getPlatId());
            // cost真的应该用买单和卖单的平均值吗？不是，用order_buy所需的资金即可
            return new EarnCost(diffPrice * amount, order_buy.getPrice() * amount);
        } else {// 如果不满足条件
            if (passArr[arrayIndex]) {// 如果早已不满足条件了
                return new EarnCost(SECOND_FAIL, 0);
            } else {// 如果是首次不满足条件
                passArr[arrayIndex] = true;
                return new EarnCost(FIRST_FAIL, 0);
            }
        }
    }

    /**
     * 精细型(穷举所有可能)：一对平台差价达不到阈值，就继续找下去。因为这不代表其他平台也达不到阈值
     *
     * @return EarnCost
     * @throws Exception 异常
     */
    private EarnCost adjustLimitPrice2(ArrayList<MarketOrder>[] totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth[0];
        List<MarketOrder> bidList = totalDepth[1];
        // log.info(askList.toString());
        // log.info("bid:====================================");
        // log.info(bidList.toString());
        // 对角线法遍历矩阵(二维数组[askList][bidList])，竖向(第一维)是askList，横向(第二维)是bidList 。
        //参见：http://shmilyaw-hotmail-com.iteye.com/blog/1769105
        // 早已不满足条件？
        boolean[] passArr = new boolean[(platList.size() - 1) * 11 + 1];//
        // adjust1是否已处理过
        boolean[] passAdjust1Arr = new boolean[(platList.size() - 1) * 11 + 1];//
        // 遍历上半个矩阵
        int maxBidIndex = Math.min(bidList.size(), maxOrderNum);
        int maxAskIndex = Math.min(askList.size(), maxOrderNum);
        int bidI = 0, askI = 0;
        Set<Integer> platIdSet = new HashSet<>();
        for (bidI = 0; bidI < maxBidIndex; bidI++) {// 横向(第二维)bidList
            for (askI = 0; askI <= bidI && askI < maxAskIndex; askI++) {// 纵向(第一维)askList
                EarnCost thisEarnCost = helpadjustLimit(askList.get(askI), bidList.get(bidI - askI), passArr, passAdjust1Arr, platIdSet, maxEarnCost);
                if (thisEarnCost.earn > FIRST_FAIL) {// 如果满足条件，才累计金额
                    maxEarnCost.earn += thisEarnCost.earn;
                    maxEarnCost.cost += thisEarnCost.cost;
                }
            }// end for
        }// end for
        if (bidI >= maxOrderNum || askI >= maxOrderNum) {
            //log.warn("市场深度不足（获取的挂单太少）");
        }
        //maxMoney需要减去平台固定费用(矿工费)
        platList.stream().filter(trade -> platIdSet.contains(trade.platId)).forEach(trade -> maxEarnCost.earn -= trade.getFixFee());
        return maxEarnCost;
    }


    /**
     * 寻找差价,生成订单.精细型(穷举所有可能)：一对平台差价达不到阈值，就继续找下去.因为这不代表其他平台也达不到阈值
     *
     * @throws Exception 异常
     */
    private EarnCost createOrders2(ArrayList<MarketOrder>[] totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth[0];
        List<MarketOrder> bidList = totalDepth[1];
        // 对角线法遍历矩阵(二维数组[askList][bidList])，竖向(第一维)是askList，横向(第二维)是bidList 。
        boolean[] passArr = new boolean[(platList.size() - 1) * 11 + 1];// 是否需要搬运
        // 遍历上半个矩阵
        int maxBidIndex = Math.min(bidList.size(), maxOrderNum);
        int maxAskIndex = Math.min(askList.size(), maxOrderNum);
        int bidI = 0, askI = 0;
        Set<Integer> platIdSet = new HashSet<>();
        for (bidI = 0; bidI < maxBidIndex; bidI++) {// 横向(第二维)bidList
            for (askI = 0; askI <= bidI && askI < maxAskIndex; askI++) {// 纵向(第一维)askList
                EarnCost thisEarnCost = helpCreateOrders(askList.get(askI), bidList.get(bidI - askI), passArr, platIdSet, maxEarnCost);
                if (thisEarnCost.earn > FIRST_FAIL) {// 如果满足条件，才累计金额
                    maxEarnCost.earn += thisEarnCost.earn;
                    maxEarnCost.cost += thisEarnCost.cost;
                }
            }// end for
        }// end for

        if (bidI >= maxOrderNum || askI >= maxOrderNum) {
            //log.warn("市场深度不足（获取的挂单太少）");
        }
        //maxMoney需要减去平台固定费用(矿工费)
        platList.stream().filter(trade -> platIdSet.contains(trade.platId)).forEach(trade -> maxEarnCost.earn -= trade.getFixFee());
        return maxEarnCost;
    }


    /**
     * 检查各平台的goods数量,如果分布不平衡,就自动转移。转移成功后，再查询账户。
     * 这个方法会造成主线程阻塞
     *
     * @return boolean 是否发生了转移
     * @throws Exception 异常
     */
    public boolean balanceTokens() throws Exception {
        if (!canBalance) {
            return false;
        }
        List<CompletableFuture<?>> balanceFutureList = new ArrayList<>();
        //分别处理两种币
        try {//todo 币安是否允许多笔提币请求同时进行？
            balanceFutureList.addAll(balanceToken(0));
            balanceFutureList.addAll(balanceToken(1));
            CompletableFuture.allOf(balanceFutureList.toArray(new CompletableFuture<?>[0])).get(60 * 10, TimeUnit.SECONDS);
        } catch (Exception e) {
        }
        return balanceFutureList.size() > 0;

    }


    private List<CompletableFuture<?>> balanceToken(int tokenIndex) throws Exception {//根据二维数组powerArr计算理想值
        List<Trade> sendTokenList = new ArrayList<>();
        List<Trade> receiveTokenList = new ArrayList<>();
        boolean needBalance = false;
        for (int platIndex = 0; platIndex < actualPlats().size(); platIndex++) {
            Trade trade = actualPlats().get(platIndex);
            double targetAmount = currentBalance.totalToken[tokenIndex] * trade.pToken[tokenIndex];
            if (trade.pToken[tokenIndex] > 0.01) {//只对有效的pToken进行处理
                //当该平台的配置参数tokenNetWork不为空，才表示开启划转
                if (trade.getTokenNetWork()[tokenIndex].length > 0) {
                    if (trade.accInfo.freeToken[tokenIndex] / targetAmount > 1) {//粗略的把每个平台划分成多方、少方
                        trade.diffToken[tokenIndex] = trade.accInfo.freeToken[tokenIndex] - targetAmount;
                        sendTokenList.add(trade);
                        if (trade.accInfo.freeToken[tokenIndex] / targetAmount > 1 + whenBalance) needBalance = true;

                    } else if (trade.accInfo.freeToken[tokenIndex] / targetAmount < 1) {
                        trade.diffToken[tokenIndex] = targetAmount - trade.accInfo.freeToken[tokenIndex];
                        receiveTokenList.add(trade);
                        if (trade.accInfo.freeToken[tokenIndex] / targetAmount < 1 - whenBalance) needBalance = true;
                    }
                }
            }
        }//end for
        List<CompletableFuture<?>> balanceFutureList = new ArrayList<>();
        if (needBalance) {// 如果需要搬运
            //while token
            while (sendTokenList.size() > 0 && receiveTokenList.size() > 0) {
                if (sendTokenList.get(0).diffToken[tokenIndex] < prop.minAmount) sendTokenList.remove(0);
                if (receiveTokenList.get(0).diffToken[tokenIndex] < prop.minAmount) receiveTokenList.remove(0);
                if (sendTokenList.size() == 0 || receiveTokenList.size() == 0) break;
                Trade t1 = sendTokenList.get(0);
                Trade t2 = receiveTokenList.get(0);
                double mindiff = Math.min(t1.diffToken[tokenIndex], t2.diffToken[tokenIndex]);
                double amount = Double.parseDouble(prop.transTokenFromat.format(mindiff));
                log.info("mindiff=" + mindiff + ", 格式化后amount=" + amount);
                //检测amount价值多少美元。如果大于10美元，才处理
                double amountValue = tokenIndex == 0 ? amount / t1.getCurrentPrice() : amount / prop.moneyPrice;
                if (amountValue > 10) {
                    //扣除双方金额，并生成转账单
                    t1.diffToken[tokenIndex] -= amount;
                    t2.diffToken[tokenIndex] -= amount;
                    balanceFutureList.add(CompletableFuture.runAsync(() -> {
                        try {
                            double receiveAmount = TransTokenUtil.trans(this, t1, t2, tokenIndex, amount);
                            log.info("最终收到" + receiveAmount + ", 损耗" + (amount - receiveAmount) + ", 损耗率" + (1 - receiveAmount / amount));
                        } catch (Exception e) {
                            log.error(t1.getPlatName() + "." + t1.token[tokenIndex] + "_" + t2.getPlatName() + "." + t2.token[tokenIndex] + "发送goods异常:", e);
                        }
                    }, threadPoolExecutor));
                } else {
                    log.info("要转移的金额小于10美元，忽略不计。amountValue=" + amountValue);
                }
            }//end while

            //【不要在这里等，而是在调用它的函数内等】等待币转移到账。最多等10分钟。如果正常结束，就必然到账了。如果没到账，系统会检测资金总量，并一直等待
            //CompletableFuture.allOf(balanceFutureList.toArray(new CompletableFuture<?>[0])).get(60 * 10, TimeUnit.SECONDS);
        }//end if
        return balanceFutureList;
    }

    /**
     * 调节goods价值占总投资额的比例。如果偏差达到一定值，就买
     *
     * @return
     * @throws Exception
     */
    public boolean balanceTokensRate() throws Exception {

        return false;
    }

    /**
     * 盘点当前余额,计算盈亏 。如果当前时间的hour符合配置文件设定的记录间隔,且当前这一小时内没记,才记录
     */
    public void saveBalance() throws Exception {
        // 盘点当前余额,计算盈亏------------------------
        // 如果当前时间的hour符合配置文件设定的记录间隔,且当前这一小时内没记,才记录
        DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH");
        DateFormat dateFmt_HH = new SimpleDateFormat("HH");
        int currentHour = Integer.parseInt(dateFmt_HH.format(new Date()));
        String currentDateHour = dateFmt.format(new Date());
        String lastBalanceDateHour = lastBalance.getDateTime().substring(0, "yyyy-MM-dd HH".length());
        /*
         * 如果(当前hour)%(时间间隔)==(起始hour)%(时间间隔),
         * 且lastBalance中的"yyyy-MM-dd HH"不等于当前的时间
         */
        if (currentHour % time_waitBalance == time_beginBalance % time_waitBalance
                && !currentDateHour.equals(lastBalanceDateHour)) {

            // 将本次余额设置为最后一次余额
            lastBalance = currentBalance;
            // 将lastBalance写入文件
            FileUtils.writeStringToFile(new File(balanceFilePath), "\n" + lastBalance.toString(), charset, true);
        }
        // end 盘点当前余额,计算盈亏-------------------
    }

    /**
     * 用当前余额,减去最近一次存储的余额,计算盈亏
     */
    public Balance getCurrentBalance() {
        Balance bal = new Balance(prop);
        // 从最后一个参数开始设置
        double totalPrice = 0;
        double totalGoods = 0;
        double totalMoney = 0;
        StringBuilder platInfo = new StringBuilder();
        for (Trade trade : actualPlats()) {
            log.debug(trade.getPlatName() + "当前价格" + trade.getCurrentPrice());
            AccountInfo inf = trade.getAccInfo();
            totalPrice += trade.getCurrentPrice();
            if (platInfo.length() != 0) {
                platInfo.append(",");
            }
            platInfo.append(trade.getPlatName()).append("Money").append(":").append(inf.totalToken[1]);
            platInfo.append(",").append(trade.getPlatName()).append("Goods").append(":").append(inf.totalToken[0]);
            totalGoods += trade.accInfo.freeToken[0];
            totalMoney += trade.accInfo.freeToken[1];
        }// end for
        bal.setPrice(totalPrice / actualPlats().size());//排除虚拟平台
        bal.setPlatInfo(platInfo.toString());
        bal.totalToken[0] = totalGoods;
        bal.totalToken[1] = totalMoney;
        //
        // 跟初始余额比较,计算总共盈亏
        Balance initBalance = new Balance(prop, firstBalance);
        double totalEarn;
        if (prop.earnMoney) {//如果是赚货币，我们认为货币会增加，商品是不会变的。
            totalEarn = bal.totalToken[1] - initBalance.totalToken[1] + bal.getPrice() * (bal.totalToken[0] - initBalance.totalToken[0]);
        } else {//如果是赚商品，我们认为商品会增加，货币是不会变的。
            totalEarn = (bal.totalToken[1] - initBalance.totalToken[1]) / bal.getPrice() + bal.totalToken[0] - initBalance.totalToken[0];
        }
        bal.setTotalEarn(totalEarn);
        // 跟上次盈亏比较,计算本次盈亏
        bal.setThisEarn(bal.getTotalEarn() - lastBalance.getTotalEarn());
        // 设置时间
        bal.setDateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return bal;
    }


    /**
     * 检查goods总数量,如果不跟初始值相等,就立即调整。
     * 跟初始值不相等的原因可能是：1.意外导致单边交易失败; 2.故意只让dex执行的，等dex执行成功再让cex在下一轮执行。
     *
     * @throws Exception 异常
     */
    public void checkTotalGoods() throws Exception {
        currentBalance = getCurrentBalance();
        Balance initBal = new Balance(prop, firstBalance);
        double diffAmount = currentBalance.totalToken[0] - initBal.totalToken[0];

        log.debug("diffAmount:" + currentBalance.totalToken[0] + " , " + initBal.totalToken[0]);
        // 如果变多,就卖.币安规定交易额最少是10美元。信息来源：CELOBUSD交易对的NOTIONAL过滤器 https://www.binance.com/api/v3/exchangeInfo
        if (diffAmount > prop.minTradeMoney / currentBalance.getPrice()) {// 如果变多,就卖.
            log.info("总goods增多" + diffAmount);
            // 增加一个虚拟的低价市场卖单，诱使程序在其他平台卖
            virtualTrade.setCurrentPrice(currentBalance.getPrice());
            // 设置市场挂单
            ArrayList<MarketOrder>[] depth = virtualTrade.getMarketDepth();
            MarketOrder marketOrder = new MarketOrder();
            marketOrder.setPlatId(virtualTrade.platId);
            marketOrder.setPrice(currentBalance.getPrice() * (1 - prop.huaDian));//价格设置不不光是在这里，还要在下一轮比价时
            marketOrder.setVolume(diffAmount);
            depth[0].add(marketOrder);
            // log.info("virtual:卖单" + depth[0].size());
            // 设置账户信息
            AccountInfo accInfo = new AccountInfo();
            accInfo.freeToken[1] = diffAmount * currentBalance.getPrice();
            virtualTrade.setAccInfo(accInfo);
        } else if (diffAmount < -prop.minTradeMoney / currentBalance.getPrice()) {// 如果变少就买
            diffAmount = 0 - diffAmount;
            log.info("总goods减少" + diffAmount);
            // 增加一个虚拟的高价市场买单，诱使程序在其他平台买
            virtualTrade.setCurrentPrice(currentBalance.getPrice());
            // 设置市场挂单
            ArrayList<MarketOrder>[] depth = virtualTrade.getMarketDepth();
            MarketOrder marketOrder = new MarketOrder();
            marketOrder.setPlatId(virtualTrade.platId);
            marketOrder.setPrice(currentBalance.getPrice() * (1 + prop.huaDian));//价格设置不不光是在这里，还要在下一轮比价时
            marketOrder.setVolume(diffAmount);
            depth[1].add(marketOrder);
            log.info("virtual:买单" + depth[1].size() + ",市场均价" + currentBalance.getPrice());
            // log.info("currentBalance.getPrice():"+currentBalance.getPrice());
            // 设置账户信息
            AccountInfo accInfo = new AccountInfo();
            accInfo.freeToken[0] = diffAmount + 10;
            virtualTrade.setAccInfo(accInfo);

        }

    }

    /**
     * 如果系统是赚goods,就检查money总数量,如果不跟初始值相等,就立即调整。
     *
     * @throws Exception 异常
     */
    public void checkTotalMoney() throws Exception {
        currentBalance = getCurrentBalance();
        Balance initBal = new Balance(prop, firstBalance);
        double diffAmount = currentBalance.totalToken[1] - initBal.totalToken[1];

        log.debug("diffAmount:" + currentBalance.totalToken[1] + " , " + initBal.totalToken[1]);
        if (diffAmount > prop.minTradeMoney) {// 如果变多,就卖
            log.info("总Money增多" + diffAmount);
            //log.info("diffAmount:" + currentBalance.totalToken[1] + " , " + initBal.totalToken[1]);

            //
        } else if (diffAmount < -prop.minTradeMoney) {// 如果变少就买
            diffAmount = 0 - diffAmount;
            log.info("总Money减少" + diffAmount);
            //log.info("diffAmount:" + currentBalance.totalToken[1] + " , " + initBal.totalToken[1]);

            //
        }

    }

    /**
     * 从xml配置文件中读取node值
     *
     * @param key 路径
     * @return 文本
     */
    private String readXmlProp(String key) {
        String path = key.replace("_", "/");
        Node node = xmlDoc.selectSingleNode("conf/" + path);
        if (node == null) {
            return "";
        } else {
            return node.getText();
        }
    }

    /**
     * 读取xml node属性
     */
    private String readXmlAttribute(String elementName, String attrName) {
        String path = elementName.replace("_", "/") + "/@" + attrName;
        Node node = xmlDoc.selectSingleNode("conf/" + path);
        if (node == null) {
            return "";
        } else {
            return node.getText();
        }
    }

    /**
     * 修改xmlDoc对象，并把xmlDoc保存到文件
     *
     * @param key   路径。如果包含下划线，会被替换成斜杠。
     * @param value 值
     * @throws Exception 异常
     */
    private void saveXmlNodeValue(String key, String value) throws Exception {
        synchronized (xmlDoc) {//对xmlDoc的写操作，可能引发线程安全问题，所以要加锁
            String path = key.replace("_", "/");
            XmlConfigUtil.saveXmlAttribute(xmlDoc, null, "conf/" + path, null, value);
        }
    }

    /**
     * 保存配置参数到文件
     *
     * @throws Exception 异常
     */
    private void saveProp2(String name1, String name2, String value) throws Exception {
        String key = name1 + "_" + name2;
        for (int i = 0; i < keyArray.length; i++) {
            if (key.equals(keyArray[i])) {
                priceArray[i] = Double.parseDouble(value);
                break;
            }
        }

        saveXmlNodeValue(key, value);

    }

    public void saveProp2(int id1, int id2, double value) throws Exception {
        int index = id1 * 10 + id2;
        priceArray[index] = value;

        saveXmlNodeValue(keyArray[index], "" + value);

    }

    private double usdrate1() throws Exception {

        String str = HttpUtil.getInstance().requestHttpGet("http://www.usd-cny.com/t3.js", "", "");
        String str2 = "price['CNY:CUR'] = ";
        int index1 = str.indexOf(str2) + str2.length();
        int index2 = str.indexOf(";", index1);
        return Double.parseDouble(str.substring(index1, index2));
    }

    private double usdrate2() throws Exception {

        String str = HttpUtil.getInstance().requestHttpGet("http://qq.ip138.com/hl.asp?from=USD&to=CNY&q=2", "", "");
        String str2 = "<td>2</td><td>";
        int index1 = str.indexOf(str2) + str2.length();
        int index2 = str.indexOf("</td>", index1);
        return Double.parseDouble(str.substring(index1, index2));
    }

    public void stopEngine() {
        stop = true;
    }

    /**
     * 手动调节参数。<b>设置后必须重启!!!</b>
     * 设置price：当两个平台之间长期不交叉时，把平台价格看作是围绕现在的价格波动。 格式 okcoin:1.2,btcchina:-1.2
     * 设置pgoods：每个平台的goods占总goods的比例
     * 设置pmoney：每个平台的money,占总money的比例
     *
     * @param key   动作：price或pgoods或pmoney
     * @param value 每个平台要设置的值，共同拼接成一个字符串：okcoin:0.2,binance:0.8
     */
    public void saveAdjustX(String key, String value) throws Exception {
        synchronized (this) {
            //检查各值总和
            double sum = Arrays.stream(value.split(",|:")).filter(o -> o.startsWith("0")).mapToDouble(Double::parseDouble).sum();
            if ((sum == 0 && key.equals("price")) || (sum == 1 && (key.equals("pgoods") || key.equals("pmoney")))
            ) {
                String[] valueArr = value.split(",");
                for (int i = 0; i < valueArr.length; i++) {//处理每一个平台
                    String adjStr = valueArr[i];
                    String[] arr = adjStr.split(":");//okcoin:0.2
                    if (arr.length != 2 || arr[1] == null || arr[1].equals("")) {//如果某平台值是空的，就跳过
                        continue;
                    } else {
                        //试着转成double，看看是否报错
                        Double.parseDouble(arr[1]);
                    }
                    switch (key) {
                        case "price":
                            //设置偏差（大标签）
                            XmlConfigUtil.saveXmlAttribute(xmlDoc, null, "conf/" + arr[0], CHANGE_PRICE, arr[1]);
                            //初始化阀值（小标签），将大标签里面的每个小标签都设为0
                            for (int j = 0; j < valueArr.length; j++) {
                                if (i != j) {
                                    String elementPath = "conf/" + arr[0] + "/" + valueArr[j].split(":")[0];
                                    XmlConfigUtil.saveXmlAttribute(xmlDoc, null, elementPath, null, "" + (0 / prop.moneyPrice));
                                }
                            }
                            break;
                        case "pgoods":
                            XmlConfigUtil.saveXmlAttribute(xmlDoc, null, "conf/" + arr[0], "pgoods", arr[1]);
                            break;
                        case "pmoney":
                            XmlConfigUtil.saveXmlAttribute(xmlDoc, null, "conf/" + arr[0], "pmoney", arr[1]);
                            break;
                        default:
                            throw new Exception("未知的动作" + key);
                    }
                }//end for
            } else {
                throw new Exception("各项值总和不合法: price总和应该是0， pgoods和pmoney总和应该是1");
            }

        }//synchronized
    }

    public String[] getEnablePlat() {
        return readXmlProp("enablePlat").split(",");
    }

    public String getFutureState() {
        return futureState;
    }

    public void setFutureState(String futureState) {
        this.futureState = futureState;

    }

    public double getOpenPriceGap() {
        return openPriceGap;
    }

    public void setOpenPriceGap(double openPriceGap) {
        this.openPriceGap = openPriceGap;
        try {
            saveXmlNodeValue("openPriceGap", openPriceGap + "");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public double getGoodsRate() {
        return goodsRate;
    }

    public void setGoodsRate(double goodsRate) {
        this.goodsRate = goodsRate;
        try {
            saveXmlNodeValue("goodsRate", goodsRate + "");
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 如果非同时挂单，就不打算从dex调节goods，那么调节goods时就没必要让dex参与
     */
    private boolean needSkipDexWhenAdjustGoods(Trade trade) {
        return !dexSync && virtualTrade.isActive() && trade.getFixFee() > 0;
    }

    /**
     * 返回全部真实的平台。不要虚拟的
     *
     * @return 真实平台
     */
    public List<Trade> actualPlats() {
        if (platList.contains(virtualTrade)) {
            return platList.subList(0, platList.size() - 1);
        } else {
            return platList;
        }
    }

}


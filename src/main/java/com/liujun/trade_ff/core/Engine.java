package com.liujun.trade_ff.core;

import com.liujun.trade_ff.core.modle.*;
import com.liujun.trade_ff.core.thread.AvgpriceThread;
import com.liujun.trade_ff.core.thread.TradeThread;
import com.liujun.trade_ff.core.util.HttpUtil;
import com.liujun.trade_ff.core.util.StringUtil;
import com.liujun.trade_ff.core.util.XmlConfigUtil;
import com.liujun.trade_ff.utils.SpringContextUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
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
    public boolean dexSync;


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

    private double openPriceGap;//开仓时，两个平台之间的差价.跟配置文件中平台出现的先后顺序有关：用前一个平台的价格减后一个平台

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

    // --------- end 对象属性 -------------------------------------------------
    static {
        log.info("in Engine static 代码块");
    }

    @PostConstruct
    public void init() {
        try {

            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(Paths.get(prop.logPath + "/conf.xml")), charset)) {
                SAXReader sax = new SAXReader();
                xmlDoc = sax.read(reader);

            } catch (Exception e) {
                log.error("engine配置文件加载异常conf.xml", e);
                throw e;
            }

            firstBalance = readXmlProp("firstBalance");

            openPriceGap = Double.parseDouble(readXmlProp("openPriceGap"));
            dexSync = Boolean.parseBoolean(readXmlProp("dexSync"));
            //---------------

            // 存放各个平台的交易对象
            platList = new ArrayList<Trade>();
            String[] paltNameArr = readXmlProp("enablePlat").split(",");
            for (String platName : paltNameArr) {//利用反射，加载各平台的对象
                Class<Trade> clazz = (Class<Trade>) Class.forName(corePackage + "." + platName + ".Trade_" + platName, true,
                        getClass().getClassLoader());
                //Trade trade = clazz.getConstructor(HttpUtil.class, int.class, double.class).newInstance(httpUtil,platList.size(), usdRate);
                Trade trade = SpringContextUtil.getBean(clazz, httpUtil, platList.size(), usdRate, prop, this);
                //如果初始化失败了
                if (!trade.initSuccess) {
                    throw new Exception(platName + ":初始化失败!!!");
                }
                //设置配置属性
                String changePriceStr = readXmlAttribute(platName, CHANGE_PRICE);
                if (StringUtil.isEmpty(changePriceStr)) {
                    changePriceStr = "0";
                }
                trade.setChangePrice(Double.parseDouble(changePriceStr));
                platList.add(trade);
            }
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
                //查询市场挂单，以及账户余额
                queryMarketDepthAndAccount(i);
                //匹配/撮合市场挂单。
                matchMarketDepth();
                //匹配/撮合备份的市场挂单,并完成交易(已确保我方有相应的资金，能吃掉这些挂单)。这两种匹配是独立的，没有关系。
                matchBackupDepth();

                // 检查goods总数量,如果不跟初始值相等,就立即买卖调整
                if (prop.earnMoney) {
                    checkTotalGoods();
                    // 检查各平台的goods数量,如果分布不平衡,就自动转移。
                    // balanceGoods();
                } else {
                    checkTotalMoney();
                    //balanceMoney();
                }

                // 盘点当前余额,计算盈亏------------------------
                saveBalance();
                //检查系统健康状况
                checkStatus(beginTime);

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
     * 查询市场深度和账户余额。并设置到trade.marketDepth属性
     *
     * @throws Exception 异常
     */
    private void queryMarketDepthAndAccount(long i) throws Exception {
        // ===== 【多线程】对各平台查询市场挂单，查询资金情况=======================
        List<CompletableFuture<?>> completableFutureList = new ArrayList<>();
        // 为每个平台启动一个线程
        for (Trade trade : platList) {
            // 设置“用户挂单”
            if (trade.getUserOrderList() == null || trade.getUserOrderList().size() > 0) {
                trade.setUserOrderList(new ArrayList<>());
            }

            completableFutureList.add(CompletableFuture.runAsync(() -> {
                try {
                    trade.flushMarketDeeps();
                } catch (Exception e) {
                    log.error(trade.getPlatName() + "查询市场深度异常:" + e.getMessage(), e);
                }
            }, threadPoolExecutor));

            //查询资金情况 -----------间隔小于3秒时，每隔3秒，查一次账户。否则每次都查
            if (time_queryOrder > 6 || (i * time_queryOrder) % 6 == 0) {
                CompletableFuture.runAsync(() -> {
                    try {
                        trade.flushAccountInfo();
                    } catch (Exception e) {
                        log.error(trade.getPlatName() + "账户查询异常:" + e.getMessage(), e);
                    }
                }, threadPoolExecutor);
            }
        }
        // 等待各个线程结束,最多等25秒.因为uniswap获取市场行情，需要8秒，重复尝试3次就有24秒-------
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture<?>[0])).get(25, TimeUnit.SECONDS);

        // ==== end【多线程】对各平台查询市场挂单=======================
    }

    /**
     * 撮合市场挂单
     *
     * @return 是否撮合成功，是否有机会盈利
     * @throws Exception 异常
     */
    private boolean matchMarketDepth() throws Exception {
        boolean isSuccess = false;
        // 设置综合深度
        MarketDepth totalDepth = new MarketDepth();
        for (Trade trade : platList) {

            totalDepth.getAskList().addAll(trade.getMarketDepth().getAskList());
            totalDepth.getBidList().addAll(trade.getMarketDepth().getBidList());

        }

        totalSort(totalDepth);// 对汇总的市场挂单进行排序：买方从大到小排序,卖方从小到大排序
        log.debug(totalDepth.getBidList() + "");/////////////////
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
                (maxEarnCost.earn >= prop.minMoney && maxEarnCost.earn / maxEarnCost.cost >= prop.atLeastRate)
        )
        ) {// 只有模拟生成的订单存在时，才搬运
            log_needTrade.info("(机会)市场最大差价" + Prop.fmt_money.get().format(maxEarnCost.diffPrice) + prop.money +
                    "，利润率" + prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) + "%," + maxEarnCost.diffPriceDirection
                    + ",最多能赚" + Prop.fmt_money.get().format(maxEarnCost.earn) + prop.money + ",模拟订单有" + maxEarnCost.orderPair + "对");

            isSuccess = true;
        } else {
            log.info("最多赚" + maxEarnCost.earn + prop.money + ",模拟订单有" + maxEarnCost.orderPair +
                    "对,利润率" + (maxEarnCost.earn > 0 ? prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) : 0) + "%," +
                    maxEarnCost.diffPriceDirection + "----------------------最大差价" + Prop.fmt_money.get().format(maxEarnCost.diffPrice) + prop.money);
        }
        return isSuccess;
    }

    /**
     * 撮合备份的市场挂单。（根据我方的资金量情况，只把跟资金量匹配的挂单保存起来。确保我方有能力吃掉这些挂单
     */
    private boolean matchBackupDepth() throws Exception {
        boolean isSuccess = false;
        // 将各平台备份的市场挂单导入汇总挂单(因为调节限价时，会把挂单的goods量改为0)
        MarketDepth totalDepth = new MarketDepth();

        for (Trade trade : platList) {
            //如果允许跨平台搬运
            if (trade.getModeLock() == 0) {
                trade.setModeLock(1);//加锁
                totalDepth.getAskList().addAll(trade.getBackupDepth().getAskList());
                totalDepth.getBidList().addAll(trade.getBackupDepth().getBidList());
            }
        }
        totalSort(totalDepth);// 对汇总的市场挂单进行排序：买方从大到小排序,卖方从小到大排序
        log.debug(totalDepth.getBidList() + "");/////////////////
        //如果平台备份的有。
        if (totalDepth.getBidList().size() > 0 && totalDepth.getAskList().size() > 0) {

            double diffPrice = totalDepth.getBidList().get(0).getPrice()
                    - totalDepth.getAskList().get(0).getPrice();
            if (diffPrice > 0) {

                log.debug("市场买单：" + totalDepth.getBidList().toString());
                log.debug("市场卖单：" + totalDepth.getAskList().toString());


                int keyIndex = totalDepth.getBidList().get(0).getPlatId() * 10
                        + totalDepth.getAskList().get(0).getPlatId();
                log.info("可搬运最大差价【" + diffPrice + "】" + keyArray[keyIndex] + " " + totalDepth.getBidList().get(0)
                        + "," + totalDepth.getAskList().get(0)); //


                EarnCost maxEarnCost = null;
                if (tradeModel.equals("simple")) {
                    maxEarnCost = createOrders1(totalDepth);// 正式生成订单
                } else if (tradeModel.equals("exact")) {
                    maxEarnCost = createOrders2(totalDepth);
                }

                //收益率要大于0.4%
                assert maxEarnCost != null;
                if (maxEarnCost.orderPair > 0 && //maxEarnCost.earn已经考虑到了矿工费
                        (maxEarnCost.earn >= prop.minMoney && maxEarnCost.earn / maxEarnCost.cost >= prop.atLeastRate)
                ) {// (正式生成的订单数量)
                    log_needTrade.info("实际能赚" + maxEarnCost.earn + prop.money + "，利润率" + prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) + "%，实际订单有" + maxEarnCost.orderPair + "对");
                    platList.forEach(Trade::processOrders);//订单预处理 。其实不需要，因为跟【查询市场挂单时执行的trade.backupUsefulOrder】方法功能是重复的.账户余额不能可不够
                    //删掉无效订单
                    int usefulOrderCount = 0;
                    for (Trade trade : platList) {
                        List<UserOrder> userOrderList = trade.getUserOrderList();
                        for (int index = userOrderList.size() - 1; index >= 0; index--) {
                            if (userOrderList.get(index).isEnable()) {
                                usefulOrderCount++;
                            } else {
                                userOrderList.remove(index);// 无效订单要及时删掉
                            }
                        }// end for
                        //如果已经加锁，判断是否应该释放锁
                        if (trade.getModeLock() == 1 && userOrderList.size() == 0) {
                            trade.setModeLock(0);
                        }

                    }
                    //检查各平台的收益率是否合规，如果全部合规，才能启动交易
                    boolean profitRateMatch = true;
                    for (Trade trade : platList) {

                        if (trade.getUserOrderList().size() > 0) {
                            trade.profitRate = trade.profitRate();
                            if (trade.profitRate < prop.atLeastRate) {
                                profitRateMatch = false;
                                log.error(trade.getPlatName() + "当前收益率" + trade.profitRate + "小于规定的收益率" + prop.atLeastRate);
                            }
                        }
                    }

                    //如果有需要执行的订单，才应该启动线程
                    if (usefulOrderCount > 0 && profitRateMatch) {
                        // 【多线程】对各平台执行挂单、查订单状态、撤销没完全成交的订单、刷新账户信息==================
                        /*1.先执行dex平台，如果成功，再执行cex平台？？？这样好吗？
                         不好：dex平台浪费了一分钟时间，这时cex平台的情况已经变动了。cex生成的订单不应该被提交，应该直接作废。
                         更好的办法是：在下一轮循环时会调节goods数量(只用cex来调节)，这样间接的执行了cex平台.
                         2.只有期货平台不需要自动调节goods，所以期货和现货，代码还是要分开的。
                         3.dex订单一提交，系统就会阻塞，直到交易被打包。如果只有dex交易成功:
                            a.如果dex消耗的是goods，系统会报money增多.平衡模块会自动买goods(从dex或cex,哪个便宜就买哪个)
                            b.如果dex消耗的是money, 会导致goods增多，平衡模块会自动卖goods(从dex或cex,哪个便宜就买哪个)
                        */
                        executeTrade();

                        // end 【多线程】对各平台执行挂单、查订单状态、撤销没完全成交的订单、刷新账户信息============
                        isSuccess = true;
                    }
                } else {//end 如果正式生成的订单数量>0
                    log.info("正式订单最多赚" + maxEarnCost.earn + prop.money + ",利润率" + (maxEarnCost.earn > 0 ? prop.formatMoney(maxEarnCost.earn / maxEarnCost.cost * 100) : 0) + "%," + maxEarnCost.orderPair + "对订单(不值得/看不上)----------------------");

                }
            }
        } else {//如果平台没有备份挂单,说明平台上没有资金
            if (totalDepth.getBidList().size() == 0) {
                log.info("平台资金" + prop.goods + "已耗尽----------------------");
            }
            if (totalDepth.getAskList().size() == 0) {
                log.info("平台资金" + prop.money + "已耗尽----------------------");
            }

        }
        //解锁
        for (Trade trade : platList) {
            if (trade.getModeLock() == 1) {
                trade.setModeLock(0);
            }
        }

        return isSuccess;
    }

    private void checkStatus(long beginTime) throws Exception {
        // 计算耗时,如果大于最大限度,就报错
        long useTime = System.currentTimeMillis() - beginTime;// 用时
        log.info("{" + currentBalance.getPlatInfo() + "}");
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
        if (currentBalance.getThisEarn() <= -60.0 / prop.moneyPrice) {
            //throw new Exception("出现亏损，暂停搬运。");
        }
    }

    /**
     * 对各交易平台，进行挂单操作
     *
     * @throws Exception 异常
     */
    private void executeTrade() throws Exception {
        List<CompletableFuture<?>> completableFutureList = new ArrayList<>();
        // 为每个平台启动一个线程--------
        //计算总的固定费用，如果>0,说明有dex平台参与，那么由dexSync参数决定是否执行cex
        double totalFixFee = platList.stream().filter(trade -> trade.getUserOrderList().size() > 0).mapToDouble(Trade::getFixFee).sum();
        for (Trade trade : platList) {
            if (trade.getUserOrderList().size() == 0)
                continue;
            /*如果有dex平台存在，跳过cex平台，只执行dex，如果dex执行成功，在下个循环通过调节goods数量，间接执行了cex。
             这样作的好处是：dex踏空率太高了，一旦dex踏空，cex也就没必要执行了，多省事啊。
             */
            if (!dexSync && totalFixFee > 0 && trade.getFixFee() == 0) {
                continue;
            }
            //把tradeThread看作runnable
            TradeThread tradeThread = SpringContextUtil.getBean(TradeThread.class, trade, this);
            completableFutureList.add(CompletableFuture.runAsync(tradeThread, threadPoolExecutor));

        }// end for
        // 等待各个线程结束,最多等time_oneCycle秒-------
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture<?>[0])).get(time_oneCycle, TimeUnit.SECONDS);
        log_haveTrade.info("===================================================================================");

        log.info("各线程都已结束=========");
    }

    /**
     * 对汇总的市场挂单进行排序：买方从大到小排序,卖方从小到大排序
     */
    private void totalSort(MarketDepth totalDepth) {
        // 卖
        Collections.sort(totalDepth.getAskList());
        // 对买方排序,然后颠倒
        Collections.sort(totalDepth.getBidList());
        Collections.reverse(totalDepth.getBidList());
        //log.info("ask挂单量"+totalDepth.getAskList().size()+",bid挂单量"+totalDepth.getBidList().size());
    }

    /**
     * 简单型：任何平台差价达不到阈值，就全部停止匹配
     *
     * @return EarnCost
     * @throws Exception 异常
     */
    private EarnCost adjustLimitPrice1(MarketDepth totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth.getAskList();
        List<MarketOrder> bidList = totalDepth.getBidList();
        // 早已不满足条件？
        boolean[] passArr = new boolean[(platList.size() - 1) * 11 + 1];//
        // adjust1是否已处理过
        boolean[] passAdjust1Arr = new boolean[(platList.size() - 1) * 11 + 1];//
        Set<Integer> platIdSet = new HashSet<>();
        while (askList.size() > 0 && bidList.size() > 0) {
            if (askList.get(0).getVolume() < prop.minCoinNum) {
                askList.remove(0);
            }
            if (bidList.get(0).getVolume() < prop.minCoinNum) {
                bidList.remove(0);
            }
            if (!(askList.size() > 0 && bidList.size() > 0)) {
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
            maxEarnCost.diffPriceDirection = keyArray[arrayIndex];
        }
        double amount = Math.min(ask.getVolume(), bid.getVolume());
        // 如果不是虚拟平台，就调节限价
        if (bid.getPlatId() != ask.getPlatId()) {
            /*
            changeLimit.adjust1(diffPrice, amount, arrayIndex, ask, bid, passAdjust1Arr);//
            changeLimit.adjust2(diffPrice, amount, arrayIndex, ask, bid);
            changeLimit.adjust3(diffPrice, amount, arrayIndex, ask, bid);
            */
        }
        // 如果有差价,并且差价大于min_diffPrice,就值得搬运
        if (diffPrice >= priceArray[arrayIndex]) {
            // 寻找两者之中较小的挂单量
            if (amount < prop.minCoinNum) {// 如果数量太小。（在简单匹配模式，数量不可能太小。太小的已经被删除了。）
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
    private EarnCost createOrders1(MarketDepth totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth.getAskList();
        List<MarketOrder> bidList = totalDepth.getBidList();

        boolean[] passArr = new boolean[(platList.size() - 1) * 11 + 1];// 是否需要搬运
        Set<Integer> platIdSet = new HashSet<>();
        while (askList.size() > 0 && bidList.size() > 0) {
            if (askList.get(0).getVolume() < prop.minCoinNum) {
                askList.remove(0);
            }
            if (bidList.get(0).getVolume() < prop.minCoinNum) {
                bidList.remove(0);
            }
            if (!(askList.size() > 0 && bidList.size() > 0)) {
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
            if (amount < prop.minCoinNum) {// 如果数量太小。（在简单匹配模式，数量不可能太小。太小的已经被删除了。）
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
    private EarnCost adjustLimitPrice2(MarketDepth totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth.getAskList();
        List<MarketOrder> bidList = totalDepth.getBidList();
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
    private EarnCost createOrders2(MarketDepth totalDepth) throws Exception {
        EarnCost maxEarnCost = new EarnCost(0, 0);// 最多能赚多少钱

        List<MarketOrder> askList = totalDepth.getAskList();
        List<MarketOrder> bidList = totalDepth.getBidList();
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
     * 检查各平台的goods数量,如果满足转移条件,就自动转移【尽量不要让转移发生】。 原则：1.预处理：设置”触发交易的最小差价“ < 单币手续费
     * 2.预处理： 通过日志观察,如果有价格倒挂,则不需要转移； 3.预处理：如果没有出现倒挂,通过日志观察是否有 (差价> 单币手续费)的时候?
     * 如果有,可以设置”触发交易的最小差价“ >= 单币手续费, 如果已设置【”触发交易的最小差价“ >=
     * 单币手续费】,则开启goods自动转移功能,等转移完了,人工反向转移money. 3.1 其他的情况,无解。(如果没有 (差价>
     * 单币手续费)的时候,则无解；)
     *
     * @throws Exception 异常
     */
    public void balanceGoods() throws Exception {
        /*
        // 计算chbtc价格:卖价、买价的平均值
        List<MarketOrder> askList_chbtc = trade_chbtc.getMarketDepth().getAskList();
        double minAskPrice_chbtc = askList_chbtc.get(askList_chbtc.size() - 1).getPrice();
        List<MarketOrder> bidList_chbtc = trade_chbtc.getMarketDepth().getBidList();
        double maxBidPrice_chbtc = bidList_chbtc.get(bidList_chbtc.size() - 1).getPrice(); // ----
        double price_chbtc = (minAskPrice_chbtc + maxBidPrice_chbtc) / 2;


        // 计算okcion价格：卖价、买价的平均值 
        List<MarketOrder> askList_okcoin = trade_okcoin.getMarketDepth().getAskList();
        double minAskPrice_okcoin = askList_okcoin.get(askList_okcoin.size() - 1).getPrice();
        List<MarketOrder> bidList_okcoin = trade_okcoin.getMarketDepth().getBidList();
        double maxBidPrice_okcoin = bidList_okcoin.get(bidList_okcoin.size() - 1).getPrice();
        double price_okcoin = (minAskPrice_okcoin + maxBidPrice_okcoin) / 2; // ----


        // 单币手续费 
        double fee = Const.formatMoney(money_Draw_rate * price_chbtc);
        // 如果chbtc上面btc占总资产的比例<30%,才需要运btc过来 
        double freeGoodsValue_chbtc = price_chbtc * trade_chbtc.getAccInfo().getFreeGoods();// btc价值多少money
        double freeMoney_chbtc = trade_chbtc.getAccInfo().getFreeMoney();
        double percent = freeGoodsValue_chbtc / (freeGoodsValue_chbtc + freeMoney_chbtc);
        percent = Const.formatMoney(percent);
        if (percent < 0.9) {// 需要搬运
        	log.warn("chbtc上面缺乏btc,需要自动搬运(btc还剩" + (percent * 100) + "%)");
        	needGoods_chbtc = true;
        	if (havePriceReverse) {// 如果有价格倒挂,则等待倒挂 //
        		log.info("等待价格倒挂........................");
        	} else {// 其他情况无解
        		log.warn("btc需要搬运,却无法搬运,无解!!!!!!");
        	}
        } else {
        	needGoods_chbtc = false;
        }
        */
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
        for (Trade trade : platList) {

            log.debug(trade.getPlatName() + "当前价格" + trade.getCurrentPrice());
            AccountInfo inf = trade.getAccInfo();
            totalPrice += trade.getCurrentPrice();
            if (platInfo.length() != 0) {
                platInfo.append(",");
            }
            platInfo.append(trade.getPlatName()).append("Money").append(":")
                    .append(inf.getFreeMoney());
            platInfo.append(",").append(trade.getPlatName()).append("Goods").append(":")
                    .append(inf.getFreeGoods());
            totalGoods += trade.getTotalGoods();
            totalMoney += trade.getTotalMoney();
        }// end for
        bal.setPrice(totalPrice / platList.size());//排除虚拟平台
        bal.setPlatInfo(platInfo.toString());
        bal.setTotalGoods(totalGoods);
        bal.setTotalMoney(totalMoney);
        //
        // 跟初始余额比较,计算总共盈亏
        Balance initBalance = new Balance(prop, firstBalance);
        double totalEarn;
        if (prop.earnMoney) {//如果是赚货币，我们认为货币会增加，商品是不会变的。
            totalEarn = bal.getTotalMoney() - initBalance.getTotalMoney()
                    + bal.getPrice() * (bal.getTotalGoods() - initBalance.getTotalGoods());
        } else {//如果是赚商品，我们认为商品会增加，货币是不会变的。
            totalEarn = (bal.getTotalMoney() - initBalance.getTotalMoney()) / bal.getPrice() +
                    bal.getTotalGoods() - initBalance.getTotalGoods();
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
        //initVirtualPlat();
        currentBalance = getCurrentBalance();
        Balance initBal = new Balance(prop, firstBalance);
        double diffAmount = currentBalance.getTotalGoods() - initBal.getTotalGoods();

        log.debug("diffAmount:" + currentBalance.getTotalGoods() + " , " + initBal.getTotalGoods());
        if (diffAmount > 1000.0 / prop.moneyPrice / currentBalance.getPrice()) {// 如果变多,就卖
            log.info("总goods增多" + diffAmount);

            //
        } else if (diffAmount < -1000.0 / prop.moneyPrice / currentBalance.getPrice()) {// 如果变少就买
            diffAmount = 0 - diffAmount;
            log.info("总goods减少" + diffAmount);

            //
        }

    }

    /**
     * 如果系统是赚goods,就检查money总数量,如果不跟初始值相等,就立即调整。
     *
     * @throws Exception 异常
     */
    public void checkTotalMoney() throws Exception {
        //initVirtualPlat();
        currentBalance = getCurrentBalance();
        Balance initBal = new Balance(prop, firstBalance);
        double diffAmount = currentBalance.getTotalMoney() - initBal.getTotalMoney();

        log.debug("diffAmount:" + currentBalance.getTotalMoney() + " , " + initBal.getTotalMoney());
        if (diffAmount > 1000.0 / prop.moneyPrice) {// 如果变多,就卖
            log.info("总Money增多" + diffAmount);
            //log.info("diffAmount:" + currentBalance.getTotalMoney() + " , " + initBal.getTotalMoney());

            //
        } else if (diffAmount < -1000.0 / prop.moneyPrice) {// 如果变少就买
            diffAmount = 0 - diffAmount;
            log.info("总Money减少" + diffAmount);
            //log.info("diffAmount:" + currentBalance.getTotalMoney() + " , " + initBal.getTotalMoney());

            //
        }

    }

    /**
     * 从xml配置文件中读取参数
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
     * 读取xml元素的属性
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
     * @param key   路径
     * @param value 值
     * @throws Exception 异常
     */
    private void saveXmlProp(String key, String value) throws Exception {
        synchronized (xmlDoc) {//对xmlDoc的写操作，可能引发线程安全问题，所以要加锁
            String path = key.replace("_", "/");
            xmlDoc.selectSingleNode("conf/" + path).setText(value);
            FileOutputStream fos = new FileOutputStream(prop.logPath + "/conf.xml", false);
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding(charset);
            XMLWriter xmlWriter = new XMLWriter(fos, format);
            xmlWriter.write(xmlDoc);
            xmlWriter.flush();
            xmlWriter.close();
            fos.close();
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

        saveXmlProp(key, value);

    }

    public void saveProp2(int id1, int id2, double value) throws Exception {
        int index = id1 * 10 + id2;
        priceArray[index] = value;

        saveXmlProp(keyArray[index], "" + value);

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
     * 手动设置偏差。<b>设置后必须重启!!!</b>当两个平台之间长期不交叉时，把平台价格看作是围绕现在的价格波动。
     *
     * @param adjustPrice 价格偏差设置。格式 okcoin:1.2,btcchina:-1.2
     */
    public void saveAdjustPrice(String adjustPrice) throws Exception {
        synchronized (this) {
            String filePath = prop.logPath + "/conf.xml";

            String[] adjustArr = adjustPrice.split(",");
            for (int i = 0; i < adjustArr.length; i++) {//处理每一个平台
                String adjStr = adjustArr[i];
                String[] arr = adjStr.split(":");// okcoin:1.2
                if (arr.length != 2 || arr[1] == null || arr[1].equals("")) {//如果某平台值是空的，就跳过
                    continue;
                } else {
                    //试着转成double，看看是否报错
                    Double.parseDouble(arr[1]);
                }
                //设置偏差（大标签）
                XmlConfigUtil.saveXmlAttribute(filePath, "conf/" + arr[0], "changePrice", arr[1]);
                //初始化阀值（小标签），将大标签里面的每个小标签都设为1
                for (int j = 0; j < adjustArr.length; j++) {
                    if (i != j) {
                        String elementPath = "conf/" + arr[0] + "/" + adjustArr[j].split(":")[0];
                        XmlConfigUtil.saveXmlProp(filePath, elementPath, "" + (1 / prop.moneyPrice));
                    }
                }
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
            saveXmlProp("openPriceGap", openPriceGap + "");
        } catch (Exception e) {
            log.error("", e);
        }
    }

}


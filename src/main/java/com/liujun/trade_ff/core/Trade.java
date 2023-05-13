package com.liujun.trade_ff.core;

import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public abstract class Trade {//goods和money放到了数组。数组中有两个元素，分别是goods和token，别搞反了
    private static final Logger log = LoggerFactory.getLogger(Trade.class);
    public final int platId;
    public final double usdRate;
    protected Prop prop;
    protected Engine engine;
    public boolean initSuccess = false;

    public String[] token = new String[2];
    public String[] tokenAddress = new String[2];
    /**
     * 最对币安，支持多种提币网络。必须指定一种一种网络。例如：uniswap运行在在arbitrum网络上，所以为了给uniswap充值，应该从币安把币提到arbitrum
     */
    public String[] tokenNetWork = new String[2];
    /**
     * 每次交易需要的固定费用(例如dex的矿工费)，单位是trade.money，例如usdt、btc
     */
    public double fixFee = 0.0;
    private String naitveToken;//仅针对dex
    /**
     * 即将要提交的订单的收益率，它一定会大于atLeastRate。这个也用来限制dex滑点
     */
    public double profitRate;
    /**
     * 为了在差价长期不出现翻转的平台之间搬运， 对查到的市场挂单，减去该价格，对要发送出的订单，加上该价格。
     */
    private double changePrice = 0.0;
    /**
     * 模式锁定：0无锁，1只能跨平台搬运 ， 2只能在自己平台内部btc/ltc/cny之间转换。因为平台内和跨平台是冲突的
     */
    private int modeLock = 0;

    /**
     * 市场深度,分别存储ask和bid
     */
    private ArrayList<MarketOrder>[] marketDepth = new ArrayList[]{new ArrayList<MarketOrder>(), new ArrayList<MarketOrder>()};

    /**
     * 备份的市场深度,分别存储ask和bid
     */
    private ArrayList<MarketOrder>[] backupDepth = new ArrayList[]{new ArrayList<MarketOrder>(), new ArrayList<MarketOrder>()};
    /**
     * 账户资产信息
     */
    public AccountInfo accInfo;
    /**
     * 当前价格。执行了flushMarketDeeps才会赋值
     */
    private double currentPrice = 1;
    public HttpUtil httpUtil;

    /**
     * 程序将要挂的单。包括买单、卖单。买单按照价格从低往高排列，卖单从高往低。
     * 这是由于helpCreateOrders()方法的机制导致的，因为这里的买单，是为了吃掉市场的卖单，而卖单价格是从低到高
     */
    private List<UserOrder> userOrderList;

    /**
     * token数量比平均值差了多少
     */
    public double[] diffToken = new double[2];


    // ==========================================================
    protected Trade(HttpUtil httpUtil, int platId, double usdRate, Prop prop, Engine engine) throws Exception {
        this.httpUtil = httpUtil;
        this.platId = platId;
        this.usdRate = usdRate;
        this.prop = prop;
        this.engine = engine;
    }

    /**
     * 获取平台的名称
     */
    public abstract String getPlatName();

    /**
     * 查询市场深度,并设置到marketDepth属性
     */
    public abstract void flushMarketDeeps() throws Exception;

    /**
     * 查询账户资产信息,并设置到accInfo属性
     */
    public abstract void flushAccountInfo() throws Exception;

    /**
     * 对市场挂单排序。买方从大到小排序,卖方从小到大排序
     */
    public void sort(ArrayList<MarketOrder>[] arrayLists) {

        Collections.sort(arrayLists[0]); // 对卖方排序，从小到大
        Collections.sort(arrayLists[1]);// 对买方排序,然后颠倒
        Collections.reverse(arrayLists[1]);

    }

    /**
     * 挂单：各平台都完成预处理后,删掉已失效的订单,对没失效的订单,进行挂单操作,并记录订单号
     *
     * @return 挂出去的订单数量
     */
    public abstract int tradeOrder() throws Exception;

    /**
     * 查出没完全成交的订单，返回数量
     */
    public abstract int queryOrderState() throws Exception;

    /**
     * 撤销没完全成交的订单
     */
    public abstract void cancelOrder() throws Exception;


    /**
     * 提取资产，发送token到外界.为什么一定要等待，直到被打包呢？因为要拿到哈希值。有了哈希值，才能调用nodeJS的receiveToken服务，进而把eth包装成weth。
     *
     * @param productName
     * @param amount
     * @param address
     * @param needWrap    只有dex需要。当dex被要求发送eth而不是weth，needWrap应该为true，这样就能把weth变成eth并发送。当dex被要求发送weth, needWrap却还是设为true,就会把eth转成weth并发送(这好像没什么意义)
     * @return txId 交易哈希
     * @throws Exception
     */
    public abstract String withdraw(String productName, double amount, String address, boolean needWrap) throws Exception;

    /**
     * 将不超出账户余额的挂单保存起来
     */
    public void backupUsefulOrder() {
        for (int i = 0; i < 2; i++) {// 处理市场ask和bid. 0代表ask, 1代表bid
            backupDepth[i].clear();
            double freeToken = accInfo.freeToken[1 - i];//处理市场卖单时，这里的freeToken指我拥有的money；反之处理市场买单，我需要出goods. 所以0和1要交换
            for (MarketOrder o : marketDepth[i]) {
                double needToken = (i == 0 ? o.getPrice() : 1) * o.getVolume();//0表示市场卖单，我需要出钱买下来.
                MarketOrder order = o.clone();
                if (freeToken >= needToken) {
                    backupDepth[i].add(order);
                    freeToken -= needToken;
                } else if (0 < freeToken) {
                    order.setVolume(freeToken / (i == 0 ? order.getPrice() : 1));
                    if (order.getVolume() >= prop.minAmount) {
                        backupDepth[i].add(order);
                    }
                    freeToken = 0.00;
                } else break;
            }
        }
    }

    /**
     * 市场挂单价格减去调整值。考虑到手续费
     */
    public void changeMarketPrice(double buyRate, double sellRate) {
        if (marketDepth[0] != null) {
            for (MarketOrder o : marketDepth[0]) {
                o.setPrice(o.getPrice() * sellRate - getChangePrice());
            }
        }
        if (marketDepth[1] != null) {
            for (MarketOrder o : marketDepth[1]) {
                o.setPrice(o.getPrice() * buyRate - getChangePrice());
            }
        }
    }

    /**
     * 为将要发送出去的挂单，加上调整值。考虑到手续费
     */
    public void changeMyOrderPrice(double buyRate, double sellRate) {
        for (UserOrder o : userOrderList) {
            if (o.getType().equals("buy")) {//如果是买单，说明跟市场卖单相对应
                o.setPrice((o.getPrice() + getChangePrice()) / sellRate);
            } else {//如果是卖单，说明跟市场买单相对应
                o.setPrice((o.getPrice() + getChangePrice()) / buyRate);
            }
            //对将要发送的挂单，调整精度
            o.setPrice(prop.formatMoney(o.getPrice()));
            o.setVolume(prop.formatGoods(o.getVolume()));
        }
    }


    /**
     * 卖 goods
     */
    public void sellGoods(double amount) throws Exception {
        setUserOrderList(new ArrayList<UserOrder>());
        UserOrder order = new UserOrder();
        double price = getCurrentPrice() - 0.43 / prop.moneyPrice;
        order.setType("sell");
        order.setPrice(price);
        order.setDiffPrice(0);
        order.setVolume(amount);
        getUserOrderList().add(order);
        // 调用订单处理
        tradeOrder();
        queryOrderState();
        cancelOrder();
        flushAccountInfo();
    }

    /**
     * 买 goods
     */
    public void buyGoods(double amount) throws Exception {
        setUserOrderList(new ArrayList<UserOrder>());
        UserOrder order = new UserOrder();
        double price = getCurrentPrice() + 0.43 / prop.moneyPrice;
        order.setType("buy");
        order.setPrice(price);
        order.setDiffPrice(0);
        order.setVolume(amount);
        getUserOrderList().add(order);
        // 调用订单处理
        tradeOrder();
        queryOrderState();
        cancelOrder();
        flushAccountInfo();
    }

    /**
     * 根据id查找订单
     */
    public UserOrder findOrderById(String id) {
        List<UserOrder> userOrderList = getUserOrderList();
        for (UserOrder order : userOrderList) {
            if (order.getOrderId().equals(id)) {
                return order;
            }
        }
        return null;
    }

    /**
     * 即将要提交的订单的收益率。这个也用来限制dex滑点。<h1>注意：要在merge()被调用之前就计算!!!!</h1>
     *
     * @return
     */
    public double profitRate() {
        double totalEarn = 0;//总利润
        double totalReserve = 0;//总交易金额
        for (UserOrder order : userOrderList) {
            /*
            //如果对方没有固定费用(矿工费),可以把滑点都放到这边。否则，滑点需要减半。因为两边都要设置滑点
            if (engine.platList.get(order.getAnotherOrder().getPlatId()).getFixFee() == 0) {
                totalEarn += order.getVolume() * order.getDiffPrice() * 0.9;
            } else {
                totalEarn += (order.getVolume() * order.getDiffPrice()) / 2.0;
            }
             */
            totalEarn += order.getVolume() * order.getDiffPrice();
            totalReserve += order.getVolume() * order.getPrice();
        }
        totalEarn -= fixFee;
        if (totalReserve > 0) {
            return Double.parseDouble(new DecimalFormat("0.0000").format(totalEarn / totalReserve));
        } else {
            return 0;
        }
    }

    //对订单进行合并。
    protected void merge() {
        log.info(getPlatName() + "存在订单:" + userOrderList.toString());
        double totalMoney = userOrderList.stream().mapToDouble(o -> o.getPrice() * o.getVolume()).sum();
        double totalVolume = userOrderList.stream().mapToDouble(UserOrder::getVolume).sum();
        //todo 买单按照价格从低往高排列，所以用最高价买，更容易成交? dex不能这样，因为容易亏损：如果你用高价买，会导致needAmountOut和slippage失去意义
        UserOrder lastOrder = userOrderList.get(userOrderList.size() - 1);
        lastOrder.setVolume(totalVolume);
        if (this.fixFee == 0) {//cex可以
            lastOrder.setPrice(lastOrder.getPrice()
                    //+ (lastOrder.getType().equals("buy") ? 1 : -1) * 0.0142 / prop.moneyPrice//为了确保成交，就提高买价，压低卖价
            );
        } else {//dex不可以
            lastOrder.setPrice(totalMoney / totalVolume);
        }
        userOrderList.clear();
        if (lastOrder.getVolume() >= prop.minTradeMoney / engine.currentBalance.getPrice()) {
            userOrderList.add(lastOrder);
        } else {
            log.warn(getPlatName() + "数量太小" + lastOrder.getVolume());
        }
    }


    // ==========getter_setter========================================================


    public double getTotalGoods() {
        return accInfo.freeToken[0] + accInfo.freezedToken[0];
    }

    public double getTotalMoney() {
        return accInfo.freeToken[1] + accInfo.freezedToken[1];
    }

    /**
     * 接收来自外界的转账。
     *
     * @param asset    资产名称，symbol
     * @param txId     交易哈希
     * @param amount   金额
     * @param needWrap 只有dex需要。当dex收到eth而不是weth，needWrap应该为true，这样就能把eth变成weth。当dex收到weth, needWrap却还是设为true,就会把weth转成eth(这好像没什么意义)
     * @return 网络确认数量。-1表示失败
     * @throws Exception
     */
    public abstract Integer depositToken(String asset, String txId, double amount, boolean needWrap) throws Exception;
}

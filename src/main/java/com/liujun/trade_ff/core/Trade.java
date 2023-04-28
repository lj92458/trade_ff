package com.liujun.trade_ff.core;

import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketDepth;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import lombok.*;
import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public abstract class Trade {
    private static final Logger log = LoggerFactory.getLogger(Trade.class);
    public final int platId;
    public final double usdRate;
    protected Prop prop;
    protected Engine engine;
    public boolean initSuccess = false;

    private String goods;
    private String goodsAddress;
    private String goodsNetWork;
    private String money;
    private String moneyAddress;
    private String moneyNetWork;
    /**
     * 每次交易需要的固定费用(例如uniswap的矿工费)，单位是trade.money，例如usdt、btc
     */
    public double fixFee = 0.0;
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
     * 市场深度
     */
    private MarketDepth marketDepth = new MarketDepth();
    /**
     * 备份的市场深度
     */
    private MarketDepth backupDepth = new MarketDepth();
    /**
     * 账户资产信息
     */
    private AccountInfo accInfo;
    /**
     * 当前价格
     */
    private double currentPrice = 1;
    public HttpUtil httpUtil;

    /**
     * 程序将要挂的单。包括买单、卖单。买单按照价格从低往高排列，卖单从高往低。
     * 这是由于helpCreateOrders()方法的机制导致的，因为这里的买单，是为了吃掉市场的卖单，而卖单价格是从低到高
     */
    private List<UserOrder> userOrderList;

    /**
     * goods数量比平均值差了多少
     */
    public double diffGoods;
    /**
     * money数量比平均值差了多少
     */
    public double diffMoney;


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
    public void sort(MarketDepth m) {

        Collections.sort(m.getAskList()); // 对卖方排序，从小到大
        Collections.sort(m.getBidList());// 对买方排序,然后颠倒
        Collections.reverse(m.getBidList());

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

    @AllArgsConstructor
    @ToString
    public static class WithdrawArgs {
        public String productName;
        public double amount;
        public String address;
    }

    /**
     * 提取Goods
     *
     * @throws Exception
     */
    public abstract void withdraw(WithdrawArgs args) throws Exception;

    /**
     * 将不超出账户余额的挂单保存起来
     */
    public void backupUsefulOrder() {
        // 处理市场卖单。如果有足够的货币余额，能将该订单买下，就将它备份起来
        backupDepth.getAskList().clear();
        double freeMoney = accInfo.getFreeMoney();
        for (MarketOrder o : marketDepth.getAskList()) {
            double needMoney = o.getPrice() * o.getVolume();
            MarketOrder order = o.clone();
            if (freeMoney >= needMoney) {
                backupDepth.getAskList().add(order);
                freeMoney -= needMoney;
            } else if (0 < freeMoney) {
                order.setVolume(freeMoney / order.getPrice());
                if (order.getVolume() >= prop.minCoinNum) {
                    backupDepth.getAskList().add(order);
                }
                freeMoney = 0.00;
            } else {
                break;
            }
        }
        // 处理市场买单。如果有足够的货物，能卖给该订单，就将它备份起来
        backupDepth.getBidList().clear();
        double freeGoods = accInfo.getFreeGoods();
        for (MarketOrder o : marketDepth.getBidList()) {
            double needGoods = o.getVolume();
            MarketOrder order = o.clone();
            if (freeGoods >= needGoods) {
                backupDepth.getBidList().add(order);
                freeGoods -= needGoods;
            } else if (0 < freeGoods) {
                order.setVolume(freeGoods);
                if (freeGoods >= prop.minCoinNum) {
                    backupDepth.getBidList().add(order);
                }
                freeGoods = 0.00;
            } else {
                break;
            }
        }
    }

    /**
     * 市场挂单价格减去调整值。考虑到手续费
     */
    public void changeMarketPrice(double buyRate, double sellRate) {
        if (marketDepth.getAskList() != null) {
            for (MarketOrder o : marketDepth.getAskList()) {
                o.setPrice(o.getPrice() * sellRate - getChangePrice());
            }
        }
        if (marketDepth.getBidList() != null) {
            for (MarketOrder o : marketDepth.getBidList()) {
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
        if (lastOrder.getVolume() >= prop.minCoinNum) {
            userOrderList.add(lastOrder);
        } else {
            log.warn(getPlatName() + "数量太小" + lastOrder.getVolume());
        }
    }


    // ==========getter_setter========================================================


    public double getTotalGoods() {
        return accInfo.getFreeGoods() + accInfo.getFreezedGoods();
    }

    public double getTotalMoney() {
        return accInfo.getFreeMoney() + accInfo.getFreezedMoney();
    }
}

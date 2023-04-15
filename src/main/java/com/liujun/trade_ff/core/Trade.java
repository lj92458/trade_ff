package com.liujun.trade_ff.core;

import com.liujun.trade_ff.core.modle.AccountInfo;
import com.liujun.trade_ff.core.modle.MarketDepth;
import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.UserOrder;
import com.liujun.trade_ff.core.util.HttpUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    /**
     * 提取Goods
     *
     * @throws Exception
     */
    public abstract void withdraw(String productName, double amount, String address) throws Exception;

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
        for (MarketOrder o : marketDepth.getAskList()) {
            o.setPrice(o.getPrice() * sellRate - getChangePrice());
        }
        for (MarketOrder o : marketDepth.getBidList()) {
            o.setPrice(o.getPrice() * buyRate - getChangePrice());
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
     * 订单预处理：对每个订单逐个检查：若账户余额不够,则将订单设为失效(backupUsefulOrder方法确保了账户余额不可能不够)。
     * 各平台预处理需要一起做，因为订单是成双成对的失效!
     *
     * @see 【不要在本方法内删除失效订单，因为删不干净】
     */
    public void processOrders() {
        List<UserOrder> userOrderList = getUserOrderList();
        // log.info(getPlatName()+"所有订单:" + userOrderList.toString());// 输出所有的订单
        AccountInfo accInfo = getAccInfo();
        // 计算总共需要多少money、goods,并记录日志
        double maxNeed_money = 0;// 最大需要的money
        double maxNeed_goods = 0;// 最大需要的goods
        double need_money = 0;// 实际需要的money
        double need_goods = 0;// 实际需要的goods
        for (int i = 0; i < userOrderList.size(); i++) {
            UserOrder order = userOrderList.get(i);
            if (order.isEnable()) {
                // 如果数量小于Const.minCoinNum，
                if (order.getVolume() < prop.minCoinNum) {
                    if (i + 1 < userOrderList.size()) {// 如果下一个订单存在，将订单合并到下一个，并设置失效
                        UserOrder nextOrder = userOrderList.get(i + 1);
                        nextOrder.setVolume(nextOrder.getVolume() + order.getVolume());
                        order.setEnable(false);
                    } else if (order.getVolume() / prop.minCoinNum >= 0.7) {// 如果不能合并，并且数量接近最小值，就修改成最小值
                        order.setVolume(prop.minCoinNum);
                        order.getAnotherOrder().setVolume(prop.minCoinNum);

                    } else {// 实在没办法，就放弃这个订单
                        order.disableOrder();
                    }

                }
                if (order.isEnable()) {// 如果数量不是太小


                    if (order.getType().equals("buy")) {// 如果是买单
                        maxNeed_money += order.getPrice() * order.getVolume();
                        double virtualRemain_money = accInfo.getFreeMoney() - need_money;// 模拟剩余金额
                        // 如果“模拟剩余额”足够
                        if ((virtualRemain_money - 1.0 / prop.moneyPrice) > order.getPrice() * order.getVolume()) {
                            need_money += order.getPrice() * order.getVolume();
                            // 否则,根据"模拟剩余金额",调整交易量
                        } else if (virtualRemain_money > 1.0 / prop.moneyPrice && (virtualRemain_money / order.getPrice()) >= prop.minCoinNum) {
                            order.changeVolume(virtualRemain_money / order.getPrice() - prop.minCoinNum);
                            need_money += virtualRemain_money;
                        } else {
                            order.disableOrder();
                            // need_money = accInfo.getFreeMoney();
                        }
                    } else {// 如果是卖单
                        maxNeed_goods += order.getVolume();
                        double virtualRemain_goods = accInfo.getFreeGoods() - need_goods;// 模拟剩余goods
                        // 如果“模拟剩goods”足够
                        if ((virtualRemain_goods - 0.0) > order.getVolume()) {
                            need_goods += order.getVolume();
                            // 否则,根据"模拟剩余goods",调整交易量
                        } else if (virtualRemain_goods >= prop.minCoinNum) {
                            order.changeVolume(virtualRemain_goods - prop.minCoinNum);
                            need_goods += virtualRemain_goods;
                        } else {
                            order.disableOrder();
                            // need_goods = accInfo.getFreeGoods();
                        }
                    }// else
                }// end if enable
            }// end if enable

        }// end for
        // 如果需要搬运
        if (maxNeed_money + maxNeed_goods > 0) {
            log.info(getPlatName() + "最多需要money:" + prop.formatMoney(maxNeed_money) + " , 最多需要goods:" + prop.formatGoods(maxNeed_goods) + "================");
            log.info(getPlatName() + "预计消耗money:" + prop.formatMoney(need_money) + " , 预计消耗goods:" + prop.formatGoods(need_goods));
            log.info(getPlatName() + "当前余额money：" + accInfo.getFreeMoney() + ",当前余额goods：" + accInfo.getFreeGoods());
            // 计算最大缺乏
            double maxLack_money = maxNeed_money - accInfo.getFreeMoney();// 缺乏多少money
            double maxLack_goods = maxNeed_goods - accInfo.getFreeGoods();// 缺乏多少goods
            if (maxLack_money > 0 || maxLack_goods > 0) {
                log.info(getPlatName() + "--------------最多缺乏money:" + prop.formatMoney(maxLack_money) + ", 最多缺乏goods:" + prop.formatGoods(maxLack_goods));
            }
            // 计算实际缺乏
            double lack_money = need_money - accInfo.getFreeMoney();// 缺乏多少money
            double lack_goods = need_goods - accInfo.getFreeGoods();// 缺乏多少goods
            if (lack_money > 0 || lack_goods > 0) {
                log.warn(getPlatName() + "--------------实际缺乏money:" + prop.formatMoney(lack_money) + ", 实际缺乏goods:" + prop.formatGoods(lack_goods));
            }

        } else {// 如果不需要搬运
            log.info(getPlatName() + "不需要搬运！");
        }

    }

    /**
     * 卖 goods
     */
    public void sellGoods(double amount) throws Exception {
        setUserOrderList(new ArrayList<UserOrder>());
        UserOrder order = new UserOrder();
        double price = getCurrentPrice() - 3.0 / prop.moneyPrice;
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
        double price = getCurrentPrice() + 3.0 / prop.moneyPrice;
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

        //将订单分成买单、卖单
        List<UserOrder> buyList = new ArrayList<UserOrder>();
        List<UserOrder> sellList = new ArrayList<UserOrder>();
        for (UserOrder order : userOrderList) {
            if (order.getType().equals("buy")) {
                buyList.add(order);
            } else {
                sellList.add(order);
            }
        }
        userOrderList.clear();

        //合并买单
        if (buyList.size() > 0) {
            log.info(getPlatName() + "存在买单:" + buyList.toString());
            double totalMoney = 0;
            double totalVolume = 0;//给一个准确的总数量，先不考虑资金不足的情况
            for (UserOrder order : buyList) {
                totalMoney += order.getPrice() * order.getVolume();
                totalVolume += order.getVolume();
            }
            UserOrder lastOrder = buyList.get(buyList.size() - 1);//todo 买单按照价格从低往高排列，所以用最高价买，更容易成交?
            /*
            double volume = (totalMoney / lastOrder.getPrice()) * 0.998;//让预备消耗的资金等于totalMoney，防止资金不足。但是这会导致成交量不足
            lastOrder.setVolume(volume);
            */
            lastOrder.setVolume(totalVolume);
            lastOrder.setPrice(lastOrder.getPrice() + 0.1 / prop.moneyPrice);//为了确保成交，就提高买价
            if (lastOrder.getVolume() >= prop.minCoinNum) {
                userOrderList.add(lastOrder);
            } else {
                log.warn(getPlatName() + "数量太小" + lastOrder.getVolume());
            }
        }
        //合并卖单
        if (sellList.size() > 0) {
            log.info(getPlatName() + "存在卖单:" + sellList.toString());
            double totalVolume = 0;
            for (UserOrder order : sellList) {
                totalVolume += order.getVolume();
            }
            UserOrder lastOrder = sellList.get(sellList.size() - 1);//todo 卖单按照价格从高往低排列，所以用最低价卖，更容易成交?

            lastOrder.setVolume(totalVolume - 0.00);
            lastOrder.setPrice(lastOrder.getPrice() - 0.1 / prop.moneyPrice);//为了确保成交，就降低卖价
            if (lastOrder.getVolume() >= prop.minCoinNum) {
                userOrderList.add(lastOrder);
            } else {
                log.warn(getPlatName() + "数量太小" + lastOrder.getVolume());
            }
        }
        //如果同时存在买单、卖单，就警告
        if (userOrderList.size() >= 2) {
            log.warn(getPlatName() + "同时存在买单、卖单:" + userOrderList.toString());
        }
        /*
        //如果是买单，只能对相同价格的合并
		if (userOrderList.size() > 1 && userOrderList.get(0).getType().equals("buy")) {
			int size1 = userOrderList.size();
			for (int i = 0; i < userOrderList.size() - 1;) {
				UserOrder order1 = userOrderList.get(i);
				UserOrder order2 = userOrderList.get(i + 1);
				if (order2.getPrice() - order1.getPrice() < 0.001) {
					order2.setVolume(order2.getVolume() + order1.getVolume());
					userOrderList.remove(i);
				} else {
					i++;
				}
			}
			int size2 = userOrderList.size();
			if (size1 > size2) {
				log.info(getPlatName() + "已合并" + (size1 - size2) + "个买单");
			}
			// 卖单，可以把所有订单合并
		} else if (userOrderList.size() > 1 && userOrderList.get(0).getType().equals("sell")) {
			int size1 = userOrderList.size();
			for (int i = 0; i < userOrderList.size() - 1;) {
				UserOrder order1 = userOrderList.get(i);
				UserOrder order2 = userOrderList.get(i + 1);
				order2.setVolume(order2.getVolume() + order1.getVolume());
				userOrderList.remove(i);
			}
			int size2 = userOrderList.size();
			if (size1 > size2) {
				log.info(getPlatName() + "已合并" + (size1 - size2) + "个卖单");
			}
		} else {// 没有订单

		}
		*/
    }


    // ==========getter_setter========================================================


    public double getTotalGoods() {
        return accInfo.getFreeGoods() + accInfo.getFreezedGoods();
    }

    public double getTotalMoney() {
        return accInfo.getFreeMoney() + accInfo.getFreezedMoney();
    }
}

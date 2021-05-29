package com.liujun.trade_ff.core.modle;


/**
 * 账户资产信息
 * 
 * @author Administrator
 * 
 */
public class AccountInfo {
	/** 账户全部资产折合货币。包括冻结的货币和货物。不包括借贷的。 */
	private double totalValue;
	/** 活动货币，
	 * 合约账户的买货能力。对于合约账户：购买力(钱货)=最大允许持仓 -已持仓量。
	 * 持有多仓，会导致钱减少(货不变)，空仓相反。
	 * 或者说：freeMoney=权益(或者余额)*持仓率-多仓仓位，freeGoods=权益(或者余额)*持仓率-空仓仓位 */
	private double freeMoney;
	/** 活动货物，
	 * 合约账户的卖货能力.对于合约账户：购买力(钱货)=最大允许持仓 -已持仓量。
	 * 持有多仓会导致钱减少(货不变)，空仓相反
	 * 或者说：freeMoney=权益(或者余额)*持仓率-多仓仓位，freeGoods=权益(或者余额)*持仓率-空仓仓位*/
	private double freeGoods;
	/** 冻结货币。
	 * 合约账户用来表示持仓的。用来检测平衡：(币本位)确保钱恒定。做空，产生钱，消耗货。做多，消耗钱，产生货。所以钱总是恒定的。 */
	private double freezedMoney;
	/** 冻结货物
	 * 合约账户用来表示持仓的。用来检测平衡：(u本位)确保货恒定。做空，产生钱，消耗货。做多，消耗钱，产生货。所以货总是恒定的。 */
	private double freezedGoods;
	/** 专为期货设置。期货的权益，或者余额.跟对方的相加, 算出totalEarn,就知道赚了多少 */
	private double totalMoney;
	/** 专为期货设置。期货的权益，或者余额.跟对方的相加, 算出totalEarn,就知道赚了多少 */
	private double totalGoods;

	public double getTotalValue() {
		return totalValue;
	}

	public void setTotalValue(double totalValue) {
		this.totalValue = totalValue;
	}

	public double getFreeMoney() {
		return freeMoney;
	}

	public void setFreeMoney(double freeMoney) {
		this.freeMoney = freeMoney;
	}

	public double getFreeGoods() {
		return freeGoods;
	}

	public void setFreeGoods(double freeGoods) {
		this.freeGoods = freeGoods;
	}

	public double getFreezedMoney() {
		return freezedMoney;
	}

	public void setFreezedMoney(double freezedMoney) {
		this.freezedMoney = freezedMoney;
	}

	public double getFreezedGoods() {
		return freezedGoods;
	}

	public void setFreezedGoods(double freezedGoods) {
		this.freezedGoods = freezedGoods;
	}

	public double getTotalMoney() {
		return totalMoney;
	}

	public void setTotalMoney(double totalMoney) {
		this.totalMoney = totalMoney;
	}

	public double getTotalGoods() {
		return totalGoods;
	}

	public void setTotalGoods(double totalGoods) {
		this.totalGoods = totalGoods;
	}
}

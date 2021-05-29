package com.liujun.trade_ff.core.thread;

import com.liujun.trade_ff.core.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SellGoodsThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(SellGoodsThread.class);
	Trade trade;
	double amount;

	public SellGoodsThread(Trade trade, double amount) {
		this.trade = trade;
		this.amount = amount;
		setName("卖goods");
	}

	public void run() {
		try {
			trade.sellGoods(amount);
		} catch (Exception e) {
			log.error(getName() + "异常", e);
		}
	}
}

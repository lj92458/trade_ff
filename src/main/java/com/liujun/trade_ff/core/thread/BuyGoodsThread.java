package com.liujun.trade_ff.core.thread;

import com.liujun.trade_ff.core.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuyGoodsThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(BuyGoodsThread.class);
	Trade trade;
	double amount;

	public BuyGoodsThread(Trade trade, double amount) {
		this.trade = trade;
		this.amount = amount;
		setName("买goods");
	}

	public void run() {
		try {
			trade.buyGoods(amount);
		} catch (Exception e) {
			log.error(getName() + "异常", e);
		}
	}

}

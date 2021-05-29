package com.liujun.trade_ff.core.thread;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 查询账户资金情况
 * 
 * @author Administrator
 * 
 */
@Component
@Scope("prototype")
public class AccountThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(Engine.class);

	// ============ 属性字段
	public Trade trade;
	private Engine engine;

	public AccountThread(Trade trade, Engine engine) {
		this.trade = trade;
		this.engine = engine;
		// 设置线程名称
		setName(trade.getPlatName() + "账户查询");
	}

	public void run() {
		try {
			trade.flushAccountInfo();

		} catch (Exception e) {
			log.error(getName() + "异常:" + e.getMessage(), e);
		}

	}

}

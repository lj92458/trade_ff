package com.liujun.trade_ff.core.thread;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MarketDepthThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(Engine.class);

	// ============ 属性字段
	public Trade trade;
	private Engine engine;
	/** 线程是否正常 */
	private boolean success = false;

	public MarketDepthThread(Trade trade, Engine engine) {
		this.trade = trade;
		this.engine = engine;
		// 设置线程名称
		setName(trade.getPlatName() + "深度");
	}

	public void run() {
		long beginTime = System.currentTimeMillis();
		try {
			trade.flushMarketDeeps();

			success = true;
		} catch (Exception e) {
			log.error(getName() + "异常:" + e.getMessage(), e);
		}

		// 计算耗时
		long endTime = System.currentTimeMillis();
		log.debug("id:" + getId() + ",线程结束,耗时" + (endTime - beginTime) + "毫秒*********************");
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

}

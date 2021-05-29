package com.liujun.trade_ff.core.modle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统计各种市场价格信息，用于自动调整各平台触发交易的最小价格。
 * 
 * @author Administrator
 * 
 */
public class PriceInfo {
	private static final Logger log = LoggerFactory.getLogger(PriceInfo.class);
	public static final double NO_BACKUP = -1000.00;

	public int platCount;
	/** 各平台最近多少次缺乏goods？ */
	public int[] lackGoodsArr;
	/** 调高限价时，备份原价 */
	public double[] backupPrice;
	/** 调高限价的时刻 */
	public long[] beginUpTime;
	/** 调低限价开始计时的时刻 */
	public long[] beginDownTime;

	/**
	 * 构造方法
	 * 
	 * @param platCount
	 *            有多少个交易平台？
	 */
	public PriceInfo(int platCount) {
		this.platCount = platCount;
		int len = (platCount - 1) * 11 + 1;
		lackGoodsArr = new int[len];
		backupPrice = new double[len];
		for (int i = 0; i < backupPrice.length; i++) {
			backupPrice[i] = NO_BACKUP;
		}
		beginUpTime = new long[len];
		beginDownTime = new long[len];
		log.info("新建PriceInfo");
	}
}

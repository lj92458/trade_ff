package com.liujun.trade_ff.core.modle;

import java.util.ArrayList;
import java.util.List;

/**
 * 市场深度
 * 
 * @author Administrator
 * 
 */
public class MarketDepth {

	/** 卖方, */
	private List<MarketOrder> askList = new ArrayList<MarketOrder>();
	/** 买方, */
	private List<MarketOrder> bidList = new ArrayList<MarketOrder>();


	public List<MarketOrder> getAskList() {
		return askList;
	}

	public void setAskList(List<MarketOrder> askList) {
		this.askList = askList;
	}

	public List<MarketOrder> getBidList() {
		return bidList;
	}

	public void setBidList(List<MarketOrder> bidList) {
		this.bidList = bidList;
	}

}

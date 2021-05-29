package com.liujun.trade_ff.vo;

import java.util.List;

/** 一条曲线图的数据 */
public class LineSerie {
	/** 曲线名称 */
	private String name;
	/** 图表类型 */
	private String type = "line";
	private String symbol="none";
	private boolean smooth=false;
	/** (数据堆叠) 各曲线数据分组求和时，本曲线属于哪个组。null表示不堆叠 */
	private String stack;//可为null
	/** 数据 */
	private List<Object> data;

	public LineSerie() {

	}

	public LineSerie(String name, String stack, List<Object> data) {
		this.name = name;
		this.stack = stack;
		this.data = data;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStack() {
		return stack;
	}

	public void setStack(String stack) {
		this.stack = stack;
	}

	public List<Object> getData() {
		return data;
	}

	public void setData(List<Object> data) {
		this.data = data;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public boolean isSmooth() {
		return smooth;
	}

	public void setSmooth(boolean smooth) {
		this.smooth = smooth;
	}

}

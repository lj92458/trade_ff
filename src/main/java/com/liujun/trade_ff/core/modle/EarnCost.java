package com.liujun.trade_ff.core.modle;

/**
 * 存储收益和成本
 */
public class EarnCost {
    public double earn;
    public double cost;
    //当前最大差价,初始化为非常小的值
    public double diffPrice = -100000000;
    //本次循环,价格最大的订单的差价方向
    public String diffPriceDirection;

    //本次循环，已配对的订单有多少对？
    public int orderPair = 0;

    public EarnCost(double earn, double cost) {
        this.earn = earn;
        this.cost = cost;

    }
}

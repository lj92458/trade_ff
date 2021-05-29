package com.liujun.trade_ff.core.uniswap.api.bean;


import java.util.List;
//list中每个元素是一个数组。数组中有两个字符串,依次代表：price,volume
public class Book {

    private List<String[]> asks;
    private List<String[]> bids;

    public Book() {
    }

    public List<String[]> getAsks() {
        return this.asks;
    }

    public void setAsks(List<String[]> asks) {
        this.asks = asks;
    }

    public List<String[]> getBids() {
        return this.bids;
    }

    public void setBids(List<String[]> bids) {
        this.bids = bids;
    }
}

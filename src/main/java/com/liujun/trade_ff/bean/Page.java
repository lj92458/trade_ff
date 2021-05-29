package com.liujun.trade_ff.bean;

import java.io.Serializable;

/**
 * 分页相关 bean
 * Created by WuShaotong on 2016/8/5 0005.
 */
public class Page implements Serializable {
    // 第几页
    private int pageIndex;
    // 每页大小
    private int pageSize;
    // 总数
    private int rowTotal;
    // 总共多少页
    private int pageTotal;


    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPagebegin() {
        int pageBegin = 0;
        pageBegin = (pageIndex - 1) * pageSize;
        if(pageBegin < 0){
            pageBegin = 0;
        }
        return pageBegin;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getRowTotal() {
        return rowTotal;
    }

    public void setRowTotal(int rowTotal) {
        this.rowTotal = rowTotal;
    }

    public int getPageTotal() {
        return pageTotal;
    }

    public void setPageTotal(int pageTotal) {
        this.pageTotal = pageTotal;
    }

}

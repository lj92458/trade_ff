package com.liujun.trade_ff.service;


import com.liujun.trade_ff.model.AvgDiff;

import java.util.List;

public interface AvgDiffService {

    /**
     * 获取某平台的价格偏差（和“各平台平均价格”之间的差距）.如果数据库中不存在某个时间点的数值，就用0填充到该点。
     * @param plat 平台名称
     * @param timeUnit 步长：每两个值之间间隔多少分钟
     * @param count 步数：获取多少个数值
     * @return
     */
    List<Object[]> selectAvgDiff(String plat, int timeUnit,int count);

    void save(AvgDiff avgDiff);
}

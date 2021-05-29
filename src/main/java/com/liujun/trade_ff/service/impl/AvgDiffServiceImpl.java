package com.liujun.trade_ff.service.impl;

import com.liujun.trade_ff.dao.AvgDiffMapper;
import com.liujun.trade_ff.model.AvgDiff;
import com.liujun.trade_ff.service.AvgDiffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AvgDiffServiceImpl implements AvgDiffService {

    @Autowired
    AvgDiffMapper avgDiffMapper;

    /*
    @Override
    public List<Double> selectAvgDiff(String plat, int timeUnit, int count) {
        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        Calendar cal = new GregorianCalendar();
        //把当前时间设在最后一个时间点上。例如timeUnit=5,当前时间是13分，那么就把当前时间设为10
        //向上取整
        cal.set(Calendar.MINUTE, (int) Math.ceil(cal.get(Calendar.MINUTE) / (double) timeUnit) * timeUnit);
        cal.add(Calendar.MINUTE, -1 * timeUnit * count);
        List<AvgDiff> avgDiffList = avgDiffMapper.select(plat, timeUnit, dfm.format(cal.getTime()));

        List<Double> diffPriceList = new ArrayList<>(count);
        //填充
        for (int i = 0; i < count; i++, cal.add(Calendar.MINUTE, timeUnit)) {
            //如果这个时间点的数据找到了
            if (avgDiffList.size() > 0 && dfm.format(cal.getTime()).equals(avgDiffList.get(avgDiffList.size() - 1).getDateTime())) {
                diffPriceList.add(avgDiffList.get(avgDiffList.size() - 1).getDiffPrice());
                avgDiffList.remove(avgDiffList.size() - 1);
            } else {
                diffPriceList.add(0d);
            }
        }

        return diffPriceList;
    }
     */

    @Override
    public List<Object[]> selectAvgDiff(String plat, int timeUnit, int count) {
        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        Calendar cal = new GregorianCalendar();
        //把当前时间设在最后一个时间点上。例如timeUnit=5,当前时间是13分，那么就把当前时间设为10
        //向上取整
        cal.set(Calendar.MINUTE, (int) Math.ceil(cal.get(Calendar.MINUTE) / (double) timeUnit) * timeUnit);
        cal.add(Calendar.MINUTE, -1 * timeUnit * count);
        List<AvgDiff> avgDiffList = avgDiffMapper.select(plat, timeUnit, dfm.format(cal.getTime()));

        List<Object[]> diffPriceList = new ArrayList<>(count);
        for(AvgDiff avg:avgDiffList){
            diffPriceList.add(new Object[]{avg.getDateTime(),avg.getDiffPrice()}) ;
        }

        return diffPriceList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = false)
    public void save(AvgDiff avgDiff) {
        avgDiffMapper.insert(avgDiff);
    }
}

package com.liujun.trade_ff.core.thread;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.model.AvgDiff;
import com.liujun.trade_ff.service.AvgDiffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 记录各平台的价格跟平均价格之间的差距。用来生成【偏差走势图】
 */
@Component
@Scope("prototype")
public class AvgpriceThread extends Thread {
    private static Logger log = LoggerFactory.getLogger(AvgpriceThread.class);
    private Engine engine;

    @Autowired
    private AvgDiffService avgDiffService;

    public AvgpriceThread() {
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public void run() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
        outerFor:
        for (; !engine.stop; ) {
            log.info("AvgpriceThread:" + this.getName() + ":" + this.getId());
            //睡眠一分钟
            try {
                sleep(1 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<Double> priceList = new ArrayList<Double>();
            List<String> nameList = new ArrayList<String>();
            double sumPrice = 0;
            boolean priceError = false;//价格是否有异常
            for (int i = 0; i < engine.platList.size(); i++) {
                Trade trade = engine.platList.get(i);
                if (trade == engine.virtualTrade) {
                    continue;
                }
                if (trade.getCurrentPrice() <= 0) {//如果价格是0 就表示异常
                    priceError = true;
                }
                double autoPrice = trade.getCurrentPrice() + trade.getChangePrice();//由于做过调整，因此要调回来
                priceList.add(autoPrice);
                nameList.add(trade.getPlatName());
                sumPrice += autoPrice;
            }//end for
            double avgPrice = sumPrice / (double) priceList.size();
            //计算差距，拼接字符串
            try {
                for (int i = 0; i < priceList.size(); i++) {
                    double diffPrice = 0;
                    if (priceError) {
                        //diffPrice = "0";//如果有异常，就把各平台价格隐藏
                        continue outerFor;//如果有异常，就放弃本次的记录
                    } else {
                        diffPrice = priceList.get(i) - avgPrice;
                    }
                    //sb.append(nameList.get(i)).append(":").append(diffPrice).append(",");
                    AvgDiff avgDiff = new AvgDiff();
                    avgDiff.setDiffPrice(diffPrice);
                    avgDiff.setPlatName(nameList.get(i));
                    avgDiff.setDateTime(dateFormat.format(new Date()));
                    avgDiffService.save(avgDiff);
                }
            } catch (Exception e) {
                log.error(this.getName() + ":" + this.getId(), e);
            }


        }// end for
    }


}

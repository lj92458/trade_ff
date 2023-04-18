package com.liujun.trade_ff.core;

import com.liujun.trade_ff.core.modle.MarketOrder;
import com.liujun.trade_ff.core.modle.PriceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 调整各平台的价格限制。 <br>
 * 1 有成交，只需确保限价不要太低。【adjust2】 <br>
 * 2.1 有价，无币，从多个平台运来？ <br>
 * 2.2.1 有币，0.4<差价<限价，逐渐调低限价。【adjust3】 <br>
 * 2.2.2 差价<0.4 ，将币搬运到其他平台? <br>
 * 2.3无差价，也无币。无需处理。 <br>
 *
 * @author Administrator
 */
@Component
public class ChangeLimit {
    /**
     * 限价调整
     */
    private static final Logger changeLimitLog = LoggerFactory.getLogger("changeLimit");
    public static final long MINUTE_20 = 20 * 60 * 1000;//调高后，回调倒计时。从20分钟改为15分钟
    public static final long HOURS_2 = 1 * 30 * 60 * 1000;//调低倒计时。从30分钟改为20分钟
    private Engine engine;

    @Autowired
    Prop prop;

    public ChangeLimit() {

    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    /**
     * 若有卖单A->B，A平台却无币可卖，且限价大于1，并且累计出现100次，则把B价设很低. (只检查卖单)<br>
     * (订单生成以后，开始调用本方法)
     */
    public void adjust1(double diffPrice, double amount, int indexA, MarketOrder ask, MarketOrder bid, boolean[] passAdjust1Arr) throws Exception {

        // 条件：该组合没有被处理过，且达到价格限制
        if (!passAdjust1Arr[indexA] && engine.priceArray[indexA] <= diffPrice) {
            passAdjust1Arr[indexA] = true;

            // 因为有了A-B方向的挂单，所以将B-A方向的计数删除
            int indexB = ask.getPlatId() * 10 + bid.getPlatId();
            String keyStrB = engine.keyArray[indexB];
            if (Engine.priceInfo.lackGoodsArr[indexB] != 0) {
                Engine.priceInfo.lackGoodsArr[indexB] = 0;
                changeLimitLog.info("清除keyStrB:" + keyStrB);
            }

            String keyStrA = engine.keyArray[indexA];
            // 如果A平台还有币可卖，就重新开始计算
            double freeGoods = engine.platList.get(bid.getPlatId()).getAccInfo().getFreeGoods();
            if (freeGoods > 0.5) {
                if (Engine.priceInfo.lackGoodsArr[indexA] != 0) {
                    Engine.priceInfo.lackGoodsArr[indexA] = 0;
                    changeLimitLog.info("清除keyStrA:" + keyStrA);
                }
                return;
            }

            // 如果A->B的限价小于1，就没必要将B->A调为负价。
            double priceA = engine.priceArray[indexA];
            if (priceA < 1.00 / prop.moneyPrice) {
                return;
            }

            // 如果满足
            int count = Engine.priceInfo.lackGoodsArr[indexA];
            if (count < (20 * 60) / engine.time_queryOrder) {// 如果没达到(20分钟)，就继续计时
                Engine.priceInfo.lackGoodsArr[indexA] += 1;
            } else {

                double priceB = engine.priceArray[indexB];
                double priceB2;
                if (priceA >= 3.5 / prop.moneyPrice) {
                    priceB2 = -1 * (priceA / 10.0);// 除以4，还是除以10 ？
                } else if (priceA >= 1.0 / prop.moneyPrice) {
                    priceB2 = -1 * (priceA / 10.0);
                } else {
                    priceB2 = priceB;//
                }
                try {
                    engine.saveProp2(ask.getPlatId(), bid.getPlatId(), priceB2);
                    changeLimitLog.info("调整限价B:" + keyStrB + ":从" + priceB + "到" + priceB2);
                    // 从新开始计算出现的次数
                    Engine.priceInfo.lackGoodsArr[indexA] = 0;
                } catch (Exception e) {
                    changeLimitLog.error("保存配置参数出现异常", e);
                }
            }// end else

        }// end if
    }

    /**
     * 突然出现高价，就追高卖。条件：条件：amount>=0.01,且0.7*(A-B) > 限价,{且A平台币数>0.5}?，且没有“调高”计时，
     * 则让限价=0.7*(A-B)。并备份原来的价格
     * 如果有调高计时，且5分钟已满，考虑是将限价恢复为【原价和当前差价】的较大者，还是将临时限价固定下来。<br>
     * (订单生成之前，开始调用本方法,否则订单都生成了，币就会被低价卖掉了。)
     *
     * @throws Exception
     */
    public void adjust2(double diffPrice, double amount, int arrayIndex, MarketOrder ask, MarketOrder bid) throws Exception {
        double freeGoods = engine.platList.get(bid.getPlatId()).getAccInfo().getFreeGoods();

        double limitPrice = prop.formatMoney(diffPrice - 0.3 * Math.abs(diffPrice));//0.7倍
        double newPrice = prop.formatMoney(diffPrice - 0.25 * Math.abs(diffPrice));//0.75倍
        long nowTime = new Date().getTime();
        long beginUpTime = Engine.priceInfo.beginUpTime[arrayIndex];
        // 条件：条件：amount>=0.01,且0.8*(A-B) > 限价,{且A平台币数>0.5}？，且没有“调高”计时，
        if (amount >= 0.5 && limitPrice > engine.priceArray[arrayIndex] && freeGoods >= 0.0 && beginUpTime == 0) {
            changeLimitLog.info("adjust2调高限价:" + engine.keyArray[arrayIndex] + "从" + engine.priceArray[arrayIndex] + "到" + newPrice);
            Engine.priceInfo.backupPrice[arrayIndex] = engine.priceArray[arrayIndex];
            // engine.priceArray[arrayIndex] = newPrice;
            engine.saveProp2(bid.getPlatId(), ask.getPlatId(), newPrice);
            Engine.priceInfo.beginUpTime[arrayIndex] = nowTime;
        }
        // 如果有调高计时，且20分钟已满，考虑是将限价恢复为【原价和当前差价】的较大者？还是将临时限价固定下来？
        if (beginUpTime != 0 && nowTime - beginUpTime > MINUTE_20) {
            if (freeGoods > 0.5) {// 若A平台币数>0.5 ,说明调的太高了，卖不出去。将限价恢复为【原价和当前差价】的较大者
                double reversePrice = Math.max(Engine.priceInfo.backupPrice[arrayIndex], diffPrice);
                changeLimitLog.info("adjust2回调限价，" + engine.keyArray[arrayIndex] + "从" + engine.priceArray[arrayIndex] + "到" + reversePrice + "币数还剩" + freeGoods);
                // engine.priceArray[arrayIndex] = reversePrice;
                engine.saveProp2(bid.getPlatId(), ask.getPlatId(), reversePrice);
            } else {// 否则说明调对了。
                changeLimitLog.info("adjust2限价调对了" + engine.keyArray[arrayIndex]);
            }
            Engine.priceInfo.backupPrice[arrayIndex] = PriceInfo.NO_BACKUP;// 删除备份的原价
            Engine.priceInfo.beginUpTime[arrayIndex] = 0;// 删除时间记录
        }
    }

    /**
     * 如果有差价，但是总是达不到限价,就调低限价。<br>
     * 条件：没有“调高”且没有“调低”临时价时，A平台币数>0.5，且
     * (A-B)小于限价*0.8，则开始计时：两小时后检查，若A平台币>0.5,就降低限价到80% <br>
     *
     * @throws Exception
     */
    public void adjust3(double diffPrice, double amount, int arrayIndex, MarketOrder ask, MarketOrder bid) throws Exception {
        double freeGoods = engine.platList.get(bid.getPlatId()).getAccInfo().getFreeGoods();

        long nowTime = new Date().getTime();
        long beginUpTime = Engine.priceInfo.beginUpTime[arrayIndex];
        long beginDownTime = Engine.priceInfo.beginDownTime[arrayIndex];
        double rate = engine.priceArray[arrayIndex] > 5.0 / prop.moneyPrice ? 0.6 : 0.6;
        double newPrice = prop.formatMoney(rate * engine.priceArray[arrayIndex]);
        // 条件：没有“调高”且没有“调低”临时价时，A平台币数>0.5，且0.4<(A-B)<限价*rate，则开始计时
        if (beginUpTime == 0 && beginDownTime == 0 && 0.5 < freeGoods && 0.35 / prop.moneyPrice <= diffPrice && diffPrice <= newPrice) {
            changeLimitLog.info("adjust3调低限价开始计时:" + engine.keyArray[arrayIndex] + "当前差价" + diffPrice + ",限价" + engine.priceArray[arrayIndex]);
            changeLimitLog.info("beginUpTime:" + beginUpTime + " , beginDownTime:" + beginDownTime + "arrayIndex:" + arrayIndex);
            Engine.priceInfo.beginDownTime[arrayIndex] = nowTime;
            changeLimitLog.info("downtime:" + Engine.priceInfo.beginDownTime[arrayIndex]);
        }
        // 条件：没有“调高”临时价，且有“调低”计时，且时间大于HOURS_2, A平台币数>0.5，且 限价*rate>= 0.4，则限价将调为80%
        if (beginUpTime == 0 && beginDownTime != 0 && nowTime - beginDownTime > HOURS_2) {
            if (freeGoods > 0.5 && 0.35 / prop.moneyPrice <= newPrice) {
                //如果当前价格比newPrice还低，就不用调整
                double finalPrice = Math.min(engine.priceArray[arrayIndex], newPrice);
                changeLimitLog.info("adjust3调低限价，" + engine.keyArray[arrayIndex] + "币数还剩" + freeGoods + "从" + engine.priceArray[arrayIndex] + "到" + finalPrice);
                engine.saveProp2(bid.getPlatId(), ask.getPlatId(), finalPrice);
            } else {
                changeLimitLog.info("adjust3无需调低限价" + engine.keyArray[arrayIndex] + "当前限价" + engine.priceArray[arrayIndex]);
            }
            Engine.priceInfo.beginDownTime[arrayIndex] = 0;// 删除时间记录
            //changeLimitLog.info("beginDownTime设为0");
        }
    }

}

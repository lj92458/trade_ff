package com.liujun.trade_ff.controller;

import com.liujun.trade_ff.core.EngineThread;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.service.AvgDiffService;
import com.liujun.trade_ff.vo.LineSerie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EngineController {
    private static Logger log = LoggerFactory.getLogger(EngineController.class);

    private EngineThread engineThread;
    @Autowired
    private AvgDiffService avgDiffService;

    @RequestMapping(value = "/engine/start", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> startEngine() {
        Map<String, String> map = new HashMap<>();
        //如果线程正在运行
        if (engineThread != null && engineThread.isAlive()) {
            map.put("retCode", "0000");
            map.put("retMsg", "程序正在运行，不能重复启动！");
        } else {
            engineThread = new EngineThread();
            //如果创建成功，才启动
            if ( engineThread.engine != null) {
                if (engineThread.engine.initSuccess) {
                    engineThread.setDaemon(true);
                    engineThread.start();
                    map.put("retCode", "0000");
                    map.put("retMsg", "启动成功！");
                } else {
                    log.error("engineThread创建失败!!!");
                    engineThread.engine.stopEngine();
                    engineThread = null;
                    map.put("retCode", "0001");
                    map.put("retMsg", "失败！");
                }
            } else {
                map.put("retCode", "0001");
                map.put("retMsg", "失败！");
            }
        }
        return map;
    }

    @RequestMapping(value = "/engine/stop", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> stopEngine() {
        Map<String, String> map = new HashMap<>();
        //如果线程正在运行
        if (engineThread != null && engineThread.isAlive()) {
            engineThread.engine.stopEngine();
            map.put("retCode", "0000");
            map.put("retMsg", "停止ok");
        } else {
            map.put("retCode", "0000");
            map.put("retMsg", "程序没有执行。不需要停止！");
        }
        return map;
    }


    @RequestMapping(value = "/engine/adjustPrice", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> saveAdjustPrice(@RequestParam String adjustPrice) {
        Map<String, String> map = new HashMap<>();
        map.put("retCode", "0000");

        stopEngine();
        try {
            engineThread.engine.saveAdjustPrice(adjustPrice);
            map.put("retMsg", "设置成功，引擎已经重启。");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            map.put("retMsg", "出现异常:" + e.getMessage());
        }
        startEngine();
        return map;
    }

    /**
     * 查询各平台的价格偏差（与平均值的差距）
     *
     * @param unit    横坐标一个单元代表几分钟
     * @param maxCell 横坐标最多有多少格
     * @return 生成曲线图需要的各参数
     */
    @RequestMapping(value = "/engine/queryDiffPrice", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> queryDiffPrice(@RequestParam int unit, @RequestParam int maxCell) {
        Map<String, Object> map = new HashMap<>();

        try {
            /*
            //打开价格偏差图时，确保引擎正在运行。
            Map resultMap = startEngine();
            if (!resultMap.get("retCode").equals("0000")) {
                throw new Exception("引擎启动失败！");
            }
            */
            if(engineThread!=null&&engineThread.engine!=null) {
                String[] legend;//样本说明

                LineSerie[] series;//多条线

                if (unit < 1 || unit > 60 || maxCell > 5000) {
                    throw new Exception("参数不合法");
                }
                //1.生成样本说明
                legend = engineThread.engine.getEnablePlat();
                //2. x坐标数据,y坐标数据
                //y
                series = new LineSerie[legend.length];
                for (int i = 0; i < legend.length; i++) {
                    String plat = legend[i];
                    List list = avgDiffService.selectAvgDiff(plat, unit, maxCell);
                    LineSerie lineSerie = new LineSerie(plat, null, list);
                    series[i] = lineSerie;

                }
                //-------------  读取盈利  ---------------
                double totalEarn = engineThread.engine.currentBalance.getTotalEarn();
                double thisEarn = engineThread.engine.currentBalance.getThisEarn();
                //
                map.put("legend", legend);

                map.put("series", series);
                map.put("totalEarn", totalEarn);
                map.put("thisEarn", thisEarn);
                //显示当前状态
                if(!engineThread.engine.stop) {
                    map.put("engineState", "正在运行");
                }else{
                    map.put("engineState", "已暂停");
                }
                //价格调整(adjustPrice)
                Map<String, Double> adjustPriceMap = new HashMap<>();
                for (Trade trade : engineThread.engine.platList) {
                    if (!trade.getPlatName().equals("virtual")) {
                        adjustPriceMap.put(trade.getPlatName(), trade.getChangePrice());
                    }

                }
                map.put("adjustPrice", adjustPriceMap);
                //上次什么时候调整的偏差？

                map.put("retCode", "0000");
            }else{
                map.put("retCode", "0001");
                map.put("retMsg", "程序没有运行，请手工启动." );
                map.put("engineState", "没有创建引擎，请启动。");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            map.put("retCode", "0001");
            map.put("retMst", "异常：" + e.getMessage());
        }

        return map;
    }


}

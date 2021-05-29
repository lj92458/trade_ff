package com.liujun.trade_ff.core;

import com.liujun.trade_ff.utils.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class EngineThread extends Thread {
    public static final Logger log = LoggerFactory.getLogger(EngineThread.class);

    public Engine engine;

    public EngineThread() {
        //新建一个engine
        engine = SpringContextUtil.getBean(Engine.class);
    }

    @Override
    public void run() {
        try {

            while (true) {

                int exitCode = 0;
                try {

                    exitCode = engine.startEngine();//如果没抛异常，那么exitCode就是1
                    if (engine.stop) {
                        break;
                    }
                    //执行完startEngine后，无论如何都要换engine，不会让它二次启动
                    engine.stopEngine();//换engine之前，要将旧的结束
                    engine = SpringContextUtil.getBean(Engine.class);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    exitCode = -2;
                }

                if (exitCode > 0) {// 如果正常退出
                    log.info("正常结束,新建engine，继续循环");
                } else {// 如果异常退出
                    log.info("出现异常,exitCode=" + exitCode + ",等待" + engine.waitSecondAfterException + "秒后继续!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    TimeUnit.SECONDS.sleep(engine.waitSecondAfterException);
                }


            }// end while
            log.info("enging:" + engine + ",engine.stop:" + engine.stop);
            log.info("while条件不满足,直接退出!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {

        }
    }//end run
}

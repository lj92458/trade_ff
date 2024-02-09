package com.liujun.trade_ff;

import com.liujun.trade_ff.core.Engine;
import com.liujun.trade_ff.core.Trade;
import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.wallet.WalletAPIService;
import com.liujun.trade_ff.core.binance.api.service.wallet.impl.WalletAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;
import com.liujun.trade_ff.core.util.TransTokenUtil;
import com.liujun.trade_ff.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("dev")
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TradeFfApplicationTests {
    String url_prex = "https://api.binance.com";
    String apiKey = "jgAwHsnY3SBLYda2z7Cikt4oezNehQREjIaKbbZWiBOI7neQSmxKh8GgFLsn1hhU";
    String secretKey = "iXkhZhtx7ouxJcSbO00LxEFsMgCbZZbePXsmWsjUFlyN7452aepCuzQ5nZAS1Jge";

    Engine engine;

    @BeforeAll
    void init() {
        engine = SpringContextUtil.getBean(Engine.class);
    }

    @Test
    void test() {
        APIConfiguration config = new APIConfiguration();
        config.setEndpoint(url_prex);
        config.setApiKey(apiKey);
        config.setSecretKey(secretKey);
        WalletAPIService walletAPIService = new WalletAPIServiceImpl(config);
        String allCoinStr = walletAPIService.queryAllCoin(DateUtils.getUnixTimeMilli());
        System.out.println(allCoinStr);
    }

    @Test
    void testCreateRoutes() throws Exception {
        List<TransTokenUtil.TransRoute> routes1 = TransTokenUtil.createRoutes(engine, engine.actualPlats().get(0), engine.actualPlats().get(2), 1);
        log.info("route1数量：" + routes1.size() + ", 内容：" + routes1);
        routes1 = TransTokenUtil.createRoutes(engine, engine.actualPlats().get(2), engine.actualPlats().get(0), 1);
        log.info("route1数量：" + routes1.size() + ", 内容：" + routes1);
        routes1 = TransTokenUtil.createRoutes(engine, engine.actualPlats().get(0), engine.actualPlats().get(2), 0);
        log.info("route1数量：" + routes1.size() + ", 内容：" + routes1);
    }

    @Test
    void testOkxDeposit() throws Exception {
        Trade trade_okcoin = engine.platList.stream().filter(t -> t.getPlatName().equals("okcoin")).findFirst().get();
        trade_okcoin.depositToken(null, "0x2d62f2a31372b45b1ef8d2f4d3c035660211b810233d3a1de4ac783bdd8acd52", 0, false);
    }

    @Test
    void testBinanceDeposit() throws Exception {
        log.info("platList.size=" + engine.platList.size());
        Trade trade_Binance = engine.platList.stream().filter(t ->
                t.getPlatName().equals("binance")
        ).findFirst().get();
        trade_Binance.depositToken(null, "0x6377b8bdb2898c3576f87a5eb2764d2a535b27c234fe28c3a82de6140a1af754", 0, false);
    }

}

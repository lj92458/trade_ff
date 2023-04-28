package com.liujun.trade_ff;

import com.liujun.trade_ff.core.binance.api.config.APIConfiguration;
import com.liujun.trade_ff.core.binance.api.service.wallet.WalletAPIService;
import com.liujun.trade_ff.core.binance.api.service.wallet.impl.WalletAPIServiceImpl;
import com.liujun.trade_ff.core.binance.api.utils.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class TradeFfApplicationTests {
    String url_prex = "https://api.binance.com";
    String apiKey = "jgAwHsnY3SBLYda2z7Cikt4oezNehQREjIaKbbZWiBOI7neQSmxKh8GgFLsn1hhU";
    String secretKey = "iXkhZhtx7ouxJcSbO00LxEFsMgCbZZbePXsmWsjUFlyN7452aepCuzQ5nZAS1Jge";

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

}

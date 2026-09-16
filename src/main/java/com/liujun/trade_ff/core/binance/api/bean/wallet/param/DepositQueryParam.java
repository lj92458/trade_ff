package com.liujun.trade_ff.core.binance.api.bean.wallet.param;

import com.liujun.trade_ff.core.binance.api.utils.DateUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositQueryParam {
    private String coin;//STRING	NO
    private Integer status;//INT	NO	0(0:pending,6: credited but cannot withdraw,7=Wrong Deposit,8=Waiting User confirm,1:success)
    private Long startTime;//	LONG	NO	默认当前时间90天前的时间戳
    private Long endTime;//	LONG	NO	默认当前时间戳
    private Integer offset;//INT	NO	默认:0
    private Integer limit;//INT	NO	默认：1000，最大1000
    private Long recvWindow;//LONG	NO
    private Long timestamp = DateUtils.getUnixTimeMilli();//LONG	YES
    private String txId;//STRING	NO

    public DepositQueryParam(long recvWindow, long timestampAdd) {
        this.recvWindow = recvWindow;
        this.timestamp += timestampAdd;
    }
}

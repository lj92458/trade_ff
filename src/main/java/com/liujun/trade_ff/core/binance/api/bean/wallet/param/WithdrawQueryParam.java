package com.liujun.trade_ff.core.binance.api.bean.wallet.param;

import com.liujun.trade_ff.core.binance.api.utils.DateUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawQueryParam {
    private String coin;//	STRING	NO
    private String withdrawOrderId;//	STRING	NO
    private Integer status;//	INT	NO	0(0:已发送确认Email,1:已被用户取消 2:等待确认 3:被拒绝 4:处理中 5:提现交易失败 6 提现完成)
    private Integer offset;//	INT	NO
    private Integer limit;//	INT	NO	默认：1000， 最大：1000
    private Long startTime;//	LONG	NO	默认当前时间90天前的时间戳
    private Long endTime;//	LONG	NO	默认当前时间戳
    private Long recvWindow;//	LONG	NO
    private Long timestamp = DateUtils.getUnixTimeMilli();//	LONG	YES

    public WithdrawQueryParam(long recvWindow, long timestampAdd) {
        this.recvWindow = recvWindow;
        this.timestamp += timestampAdd;
    }
}

package com.liujun.trade_ff.core.binance.api.bean.wallet.result;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositQueryResult {

    private String id;
    private String amount;
    private String coin;
    private String network;
    /**
     * 0:pending,6: credited but cannot withdraw,7=Wrong Deposit,8=Waiting User confirm,1:success
     */
    private int status;
    private String address;
    private String addressTag;
    private String txId;
    private long insertTime;
    private int transferType;
    private String confirmTimes;
    private int unlockConfirm;
    private int walletType;
}

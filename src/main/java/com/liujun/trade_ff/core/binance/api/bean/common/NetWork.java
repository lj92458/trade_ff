package com.liujun.trade_ff.core.binance.api.bean.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NetWork {
    private String network; //ETH",
    private String coin; //AGLD",
    private double withdrawIntegerMultiple; //0.00000001",
    private boolean isDefault;// true,
    private boolean depositEnable;// true,
    private boolean withdrawEnable;// true,
    private String depositDesc; //",
    private String withdrawDesc; //",
    private String specialTips; //",
    private String specialWithdrawTips; //",
    private String name; //Ethereum (ERC20)",
    private boolean resetAddressStatus;// false,
    private String addressRegex; //^(0x)[0-9A-Fa-f]{40}$",
    private String addressRule; //",
    private String memoRegex; //",
    private double withdrawFee; //13",
    private double withdrawMin; //26",
    private double withdrawMax; //9999999",
    private double minConfirm;// 12,
    private double unLockConfirm;// 64,
    private boolean sameAddress;// false,
    private double estimatedArrivalTime;// 5,
    private boolean busy;// false,
    private String country; //AE,BINANCE_BAHRAIN_BSC,custody,KZ",
    private String contractAddressUrl; //https://etherscan.io/token/",
    private String contractAddress; //0x32353a6c91143bfd6c7d363b546e62a9a2489a20"
}

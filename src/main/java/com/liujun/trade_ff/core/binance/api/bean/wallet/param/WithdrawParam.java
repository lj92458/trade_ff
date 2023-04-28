package com.liujun.trade_ff.core.binance.api.bean.wallet.param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@ToString
@Getter
@Setter
public class WithdrawParam {

    private String coin;//yes
    private String withdrawOrderId;//no	自定义提币ID
    private String network;//no	提币网络
    private String address;//yes	提币地址
    private String addressTag;//no	某些币种例如 XRP,XMR 允许填写次级地址标签
    private Double amount;//yes
    private Boolean transactionFeeFlag;//no	当站内转账时免手续费, true: 手续费从转入方扣(收到的比预计的少); false: 手续费从转出方扣(你发送多少，对方就收到多少); 默认 false
    private String name;//no	地址的备注，填写该参数后会加入该币种的提现地址簿。地址簿上限为20，超出后会造成提现失败。
    private Integer walletType = 0;//no 表示出金使用的钱包，0为现货钱包，1为资金钱包。默认walletType为"充币账户"是您设置在钱包->现货账户或资金账户->充值
    private Long recvWindow;//no
    private Long timestamp;//yes

    public WithdrawParam(String coin, String address, Double amount, Long timestamp) {
        this.coin = coin;
        this.address = address;
        this.amount = amount;
        this.timestamp = timestamp;
    }

}

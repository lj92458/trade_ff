package com.liujun.trade_ff.core.binance.api.bean.wallet.result;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawQueryResult {
    private String id;//"b6ae22b3aa844210a7041aee7589627c",  // 该笔提现在币安的id
    private String amount;//"8.91000000",   // 提现转出金额
    private String transactionFee;//"0.004", // 手续费
    private String coin;//"USDT",
    private int status;// 6, 列举 0:已发送确认Email,1:已被用户取消 2:等待确认 3:被拒绝 4:处理中 5:提现交易失败 6 提现完成
    private String address;//"0x94df8b352de7f46f64b01d3666bf6e936e44ce60",
    private String txId;//"0xb5ef8c13b968a406cc62a93a8bd80f9e9a906ef1b3fcf20a2e48573c17659268"   // 提现交易id
    private String applyTime;//"2019-10-12 11;//12;//02",  // UTC 时间
    private String network;//"ETH",
    private int transferType;// 0 // 1;// 站内转账, 0;// 站外转账
    private String withdrawOrderId;//"WITHDRAWtest123", // 自定义ID, 如果没有则不返回该字段
    private String info;//"The address is not valid. Please confirm with the recipient",  // 提币失败原因
    private int confirmNo;//3,  // 提现确认数
    private int walletType;// 1,  //1;// 资金钱包 0;//现货钱包
    private String txKey;//"",
    private String completeTime;//"2023-03-23 16;//52;//41"  // 提现完成，成功下账时间(UTC)
}

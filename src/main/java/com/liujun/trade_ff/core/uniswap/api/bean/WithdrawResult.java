package com.liujun.trade_ff.core.uniswap.api.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WithdrawResult {
    String msg;
    boolean success;
    String orderId;//交易哈希

}

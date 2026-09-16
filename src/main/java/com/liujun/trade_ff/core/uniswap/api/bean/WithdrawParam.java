package com.liujun.trade_ff.core.uniswap.api.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@ToString
@Getter
@Setter
public class WithdrawParam {
    String asset;//
    String address;//	提币地址
    Double amount;//
    /**
     * weth要转成eth？ eth也要转成weth? 【仅针对eth和weth有意义，对usdc无意义】
     * 如果需要，当symbol是eth时，会自动把weth转成eth并发送;当symbol是weth时，会自动把eth转成weth并发送.
     */
    boolean needWrap;


}

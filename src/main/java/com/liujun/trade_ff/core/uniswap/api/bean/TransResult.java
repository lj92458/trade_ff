package com.liujun.trade_ff.core.uniswap.api.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransResult {
    String orderId;//就是交易hash
    int nonce;
    String hash;

}

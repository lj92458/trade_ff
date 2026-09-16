package com.liujun.trade_ff.core.uniswap.api.bean;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class TransResult {
    String orderId;//就是交易hash
    int nonce;
    String hash;

}

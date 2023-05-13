package com.liujun.trade_ff.core.okcoinF;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Instruments {
    private String instrument_id;
    private String underlying;
    private String base_currency;
    private String quote_currency;
    private String settlement_currency;
    private String contract_val;
    private String listing;
    private String delivery;
    private String tick_size;
    private String alias;
    private String is_inverse;
    private String contract_val_currency;
    private String trade_increment;
}

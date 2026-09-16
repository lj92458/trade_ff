package com.liujun.trade_ff.core.binance.api.bean.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoinInfo {
    private String coin;//"AGLD",
    private boolean depositAllEnable;//true,
    private boolean withdrawAllEnable;//true,
    private String name;//"Adventure Gold",
    private double free;//"0",
    private double locked;//"0",
    private double freeze;//"0",
    private double withdrawing;//"0",
    private double ipoing;//"0",
    private double ipoable;//"0",
    private double storage;//"0",
    private boolean isLegalMoney;//false,
    private boolean trading;//true,
    private List<NetWork> networkList;//
}

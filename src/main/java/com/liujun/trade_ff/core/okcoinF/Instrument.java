package com.liujun.trade_ff.core.okcoinF;

import com.okcoin.commons.okex.open.api.bean.futures.result.Instruments;

public class Instrument {
    private Instruments instruments;


    public Instrument(Instruments instruments) {
        this.instruments = instruments;
    }

//    //get set
//    public int getCategory() {
//        return Integer.parseInt(instruments.getCategory());
//    }


//    public String getUnderlying_index() {
//        return instruments.getUnderlying_index();
//    }


    public String getInstrument_id() {
        return instruments.getInstrument_id();
    }


    public String getUnderlying() {
        return instruments.getUnderlying();
    }


    public String getBase_currency() {
        return instruments.getBase_currency();
    }


    public String getQuote_currency() {
        return instruments.getQuote_currency();
    }


    public String getSettlement_currency() {
        return instruments.getSettlement_currency();
    }


    public double getContract_val() {
        return Double.parseDouble(instruments.getContract_val());
    }


    public String getListing() {
        return instruments.getListing();
    }


    public String getDelivery() {
        return instruments.getDelivery();
    }

    /** tick_size(价格精度)是指下单价格的最小增量，委托价格必须是tick_size的倍数*/
    public double getTick_size() {
        return Double.parseDouble(instruments.getTick_size());
    }


    public String getAlias() {
        return instruments.getAlias();
    }


    public boolean getIs_inverse() {
        return Boolean.parseBoolean(instruments.getIs_inverse());
    }


    public String getContract_val_currency() {
        return instruments.getContract_val_currency();
    }

    /** 下单数量精度 */
    public double getTrade_increment() {
        return Double.parseDouble(instruments.getTrade_increment());
    }
}

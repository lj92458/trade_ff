package com.liujun.trade_ff.core.uniswap.api.bean;

public class WithdrawParam {
    String asset;//


    String address;//	提币地址

    Double amount;//

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}

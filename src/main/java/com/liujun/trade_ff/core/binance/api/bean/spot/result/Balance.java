package com.liujun.trade_ff.core.binance.api.bean.spot.result;

public class Balance {
    private String asset;
    private String free;
    private String locked;

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getAsset() {
        return asset;
    }

    public void setFree(String free) {
        this.free = free;
    }

    public String getFree() {
        return free;
    }

    public void setLocked(String locked) {
        this.locked = locked;
    }

    public String getLocked() {
        return locked;
    }
}

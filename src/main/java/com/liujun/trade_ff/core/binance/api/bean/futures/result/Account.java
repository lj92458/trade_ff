package com.liujun.trade_ff.core.binance.api.bean.futures.result;

import java.util.List;

public class Account {
    //https://binance-docs.github.io/apidocs/delivery/cn/#user_data-7

    private List<Asset> assets;
    private List<Position> positions;

    private boolean canDeposit;//: true, // 是否可以入金
    private boolean canTrade;//: true, // 是否可以交易
    private boolean canWithdraw;//: true, // 是否可以出金
    private int feeTier;//: 2, // 手续费等级
    private long updateTime;//: 0

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    public boolean isCanDeposit() {
        return canDeposit;
    }

    public void setCanDeposit(boolean canDeposit) {
        this.canDeposit = canDeposit;
    }

    public boolean isCanTrade() {
        return canTrade;
    }

    public void setCanTrade(boolean canTrade) {
        this.canTrade = canTrade;
    }

    public boolean isCanWithdraw() {
        return canWithdraw;
    }

    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }

    public int getFeeTier() {
        return feeTier;
    }

    public void setFeeTier(int feeTier) {
        this.feeTier = feeTier;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}

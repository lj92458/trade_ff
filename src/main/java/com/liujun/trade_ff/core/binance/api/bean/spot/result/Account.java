package com.liujun.trade_ff.core.binance.api.bean.spot.result;

import java.util.List;

public class Account {
    private int makerCommission;
    private int takerCommission;
    private int buyerCommission;
    private int sellerCommission;
    private boolean canTrade;
    private boolean canWithdraw;
    private boolean canDeposit;
    private long updateTime;
    private String accountType;
    private List<Balance> balances;
    private List<String> permissions;
    public void setMakerCommission(int makerCommission) {
        this.makerCommission = makerCommission;
    }
    public int getMakerCommission() {
        return makerCommission;
    }

    public void setTakerCommission(int takerCommission) {
        this.takerCommission = takerCommission;
    }
    public int getTakerCommission() {
        return takerCommission;
    }

    public void setBuyerCommission(int buyerCommission) {
        this.buyerCommission = buyerCommission;
    }
    public int getBuyerCommission() {
        return buyerCommission;
    }

    public void setSellerCommission(int sellerCommission) {
        this.sellerCommission = sellerCommission;
    }
    public int getSellerCommission() {
        return sellerCommission;
    }

    public void setCanTrade(boolean canTrade) {
        this.canTrade = canTrade;
    }
    public boolean getCanTrade() {
        return canTrade;
    }

    public void setCanWithdraw(boolean canWithdraw) {
        this.canWithdraw = canWithdraw;
    }
    public boolean getCanWithdraw() {
        return canWithdraw;
    }

    public void setCanDeposit(boolean canDeposit) {
        this.canDeposit = canDeposit;
    }
    public boolean getCanDeposit() {
        return canDeposit;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    public long getUpdateTime() {
        return updateTime;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    public String getAccountType() {
        return accountType;
    }

    public void setBalance(List<Balance> balances) {
        this.balances = balances;
    }
    public List<Balance> getBalance() {
        return balances;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    public List<String> getPermissions() {
        return permissions;
    }
}

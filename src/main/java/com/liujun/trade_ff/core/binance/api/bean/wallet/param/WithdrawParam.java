package com.liujun.trade_ff.core.binance.api.bean.wallet.param;

public class WithdrawParam {

    String asset;//
    String withdrawOrderId;//	自定义提币ID
    String network;//	提币网络
    String address;//	提币地址
    String addressTag;//	某些币种例如 XRP,XMR 允许填写次级地址标签
    Double amount;//
    Boolean transactionFeeFlag;//	当站内转账时免手续费, true: 手续费归资金转入方; false: 手续费归资金转出方; . 默认 false.
    String name;//	地址的备注，填写该参数后会加入该币种的提现地址簿。地址簿上限为20，超出后会造成提现失败。
    Long recvWindow;//
    Long timestamp;//

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getWithdrawOrderId() {
        return withdrawOrderId;
    }

    public void setWithdrawOrderId(String withdrawOrderId) {
        this.withdrawOrderId = withdrawOrderId;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressTag() {
        return addressTag;
    }

    public void setAddressTag(String addressTag) {
        this.addressTag = addressTag;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Boolean getTransactionFeeFlag() {
        return transactionFeeFlag;
    }

    public void setTransactionFeeFlag(Boolean transactionFeeFlag) {
        this.transactionFeeFlag = transactionFeeFlag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRecvWindow() {
        return recvWindow;
    }

    public void setRecvWindow(Long recvWindow) {
        this.recvWindow = recvWindow;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

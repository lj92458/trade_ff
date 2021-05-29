package com.liujun.trade_ff.core.uniswap.api.bean;

public class APIConfiguration {
    private String uri;//远程调用，该访问那个uri

    private String address;//以太账户地址
    private int maxWaitSeconds;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getMaxWaitSeconds() {
        return maxWaitSeconds;
    }

    public void setMaxWaitSeconds(int maxWaitSeconds) {
        this.maxWaitSeconds = maxWaitSeconds;
    }
}

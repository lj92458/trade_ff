package com.liujun.trade_ff.bean;

/**
 * 自动登录 相关bean
 * Created by WuShaotong on 2016/12/28.
 */
public class AutoSigninBean {
    private String userAccount;         //用户账号
    private String userAuth;            //用户身份标识 浏览器UA等信息加密处理
    private String expireTime;          //到期时间 格式 yyyy-MM-dd HH:mm:ss

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(String userAuth) {
        this.userAuth = userAuth;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }
}

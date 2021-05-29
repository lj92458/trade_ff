package com.liujun.trade_ff.vo;

import java.io.Serializable;

/**
 * 登录用 Value Object
 * Created by WuShaotong on 2016/8/10.
 */
public class SignInVO implements Serializable {
    private String account;
    private String password;
    private String verifyCode;
    private Boolean isAutoSign;

    private String errMsg;
    private String errMsgAccount;
    private String errMsgPassword;
    private String errMsgVerifyCode;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public Boolean getIsAutoSign() {
        return isAutoSign;
    }

    public void setIsAutoSign(Boolean autoSign) {
        isAutoSign = autoSign;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getErrMsgAccount() {
        return errMsgAccount;
    }

    public void setErrMsgAccount(String errMsgAccount) {
        this.errMsgAccount = errMsgAccount;
    }

    public String getErrMsgPassword() {
        return errMsgPassword;
    }

    public void setErrMsgPassword(String errMsgPassword) {
        this.errMsgPassword = errMsgPassword;
    }

    public String getErrMsgVerifyCode() {
        return errMsgVerifyCode;
    }

    public void setErrMsgVerifyCode(String errMsgVerifyCode) {
        this.errMsgVerifyCode = errMsgVerifyCode;
    }
}

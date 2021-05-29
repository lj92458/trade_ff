package com.liujun.trade_ff.common.exception;

/**
 * 业务异常
 * Created by WuShaotong on 2016/8/9.
 */
public class BizException extends RuntimeException{

    private String errCode;
    private String errMsg;

    public BizException(){
        super();
    }

    public BizException(String message){
        super(message);
    }

    public BizException(String errorCode, String errorMsg){
        super("Error [" + errorCode + "] " + errorMsg);
        this.errCode = errorCode;
        this.errMsg = errorMsg;
    }

    public String getErrCode() {
        return errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }
}

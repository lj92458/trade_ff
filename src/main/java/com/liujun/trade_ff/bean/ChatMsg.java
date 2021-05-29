package com.liujun.trade_ff.bean;


/**
 * Created by Administrator on 2017/3/22.
 */
public class ChatMsg {

    public ChatMsg() {
    }


    //房间号
    private String roomId;
    //用户帐号
    private String accountNo;
    //用户名
    private String nickName;
    /**
     * roomId + "_" + accountNo
     */
    private String userIdentity;
    //当前系统时间
    private String curTime;
    //聊天内容
    private String chatContent;
    /**
     * 是否是系统消息。1是，0不是
     */
    private String isSysMsg;

    public String getIsSysMsg() {
        return isSysMsg;
    }

    public void setIsSysMsg(String isSysMsg) {
        this.isSysMsg = isSysMsg;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }


    public String getCurTime() {
        return curTime;
    }

    public void setCurTime(String curTime) {
        this.curTime = curTime;
    }

    public String getChatContent() {
        return chatContent;
    }

    public void setChatContent(String chatContent) {
        this.chatContent = chatContent;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getUserIdentity() {
        return userIdentity;
    }

    public void setUserIdentity(String userIdentity) {
        this.userIdentity = userIdentity;
    }
}

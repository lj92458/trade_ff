/**
 * Created by Administrator on 2017/3/24.
 */
$(document).ready(function () {
    stomp_connect('rootLog');
});

var stompClient = null;

/**
 * 前台的连接方法
 * @param roomId
 * @param nickName
 * @param accountNo
 * @param welcome
 * @param callback
 */

function stomp_connect(roomId, callback) {
    if (stompClient == null) {
        var socket = new SockJS('./webchat');
        stompClient = Stomp.over(socket);
        stompClient.connect({}, function (frame) {

            //用户订阅某房间的消息。
            stompClient.subscribe("/topic/" + roomId, function (chat) {
                showMsg3(roomId, chat.body);

                //call back
                if (callback) {
                    callback();
                }
            });

        });

    }
}


/**
 * 发送消息。暂时用不上
 * @param isSysMsg 是否是系统消息
 * @param textMsg 消息内容
 */
function sendMessage(isSysMsg, textMsg) {
    //如果还没连接

    if (userStatus == 1) {//cookie中有会员,但是会员没有登录
        alert('请使用注册过的账号登录, 才能发言!');
        return;
    } else {
        var chatCont = textMsg;
        if (chatCont) {
            //去除多余的空格，换行符。
            chatCont = chatCont.replace(/\t+/g, '\t');
            chatCont = chatCont.replace(/ +/g, ' ');
            chatCont = chatCont.replace(/\r/g, '');
            chatCont = chatCont.replace(/\n+/g, '\n');
            stompClient.send("/app/processMsg", {}, JSON.stringify({
                'roomId': roomId,
                'accountNo': accountNo,
                'nickName': nickName,
                'chatContent': chatCont,
                'isSysMsg': isSysMsg,
                'userIdentity': roomId + '_' + accountNo

            }));
        } else {
            alert('禁止发送空消息！');
        }
    }


}

function stomp_disconnect() {

    //客户端关闭页面时，断开连接，关闭聊天室
    if (stompClient != null) {
        stompClient.disconnect();
        stompClient = null;
    }


}


/**
 * 客户端和收到消息后，显示在客户端页面
 * @param message
 */

function showMsg3(roomId, message) {

    var logDiv = $('#' + roomId);
    var subDivs = logDiv.find('div');
    if (subDivs.length >= 10) {
        subDivs.first().remove();
    }
    logDiv.append('<div>' + message + '</div>');
}



package com.liujun.trade_ff.controller;


import com.liujun.trade_ff.bean.ChatMsg;
import com.liujun.trade_ff.bean.ChatRoom;
import com.liujun.trade_ff.bean.RoomContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天室
 * 参考 http://www.myexception.cn/web/1803222.html
 */
@Slf4j
@Controller
public class UserChatController {
    //每个聊天室缓存最大聊天信息条数，该值由SpringMVC的配置文件注入，超过该值将清理出缓存
    @Value("${chat.maxHistory}")
    private int maxChatHistory;


    //防止跨站攻击，需要被替换的字符
    private Map<String, String> xssMap = new HashMap<String, String>();
    private SimpMessagingTemplate template;
    //消息缓存列表
//    private Map<String, ChatRoom> roomMap = new HashMap<String, ChatRoom>();


    private static final String GIFT_MESSAGE_FLAG = "__GM__";     //礼物消息标识


    @Autowired
    public UserChatController(SimpMessagingTemplate t) {//这里不算错。
        template = t;
        //logger.info("=================SimpMessagingTemplate:" + t);
        xssMap.put("[s|S][c|C][r|R][i|I][p|P][t|T]", "");
        // 含有脚本 javascript
        xssMap.put("[\\\"\\\'][\\s]*[j|J][a|A][v|V][a|A][s|S][c|C][r|R][i|I][p|P][t|T]:(.*)[\\\"\\\']", "");
        // 含有函数： eval
        xssMap.put("[e|E][v|V][a|A][l|L]\\((.*)\\)", "");
        // 含有符号 <
        xssMap.put("<", "&lt;");
        // 含有符号 >
        xssMap.put(">", "&gt;");
        // 含有符号 (
        xssMap.put("\\(", "&#40;");
        // 含有符号 )
        xssMap.put("\\)", "&#41;");
        // 含有符号 '
        xssMap.put("'", "&#39;");
        // 含有符号 "
        xssMap.put("\"", "&quot;");
        // 含有符号 \
        xssMap.put("\\\\", "&#92;");
    }

    /**
     * @param chatMsg 关于用户聊天的各个信息
     */
    @MessageMapping("/processMsg")
    //法方的返回值，默认会发送到【/topic + @MessageMapping指定路径】组合而成的路径，即/topic/processMsg。
    //当然也可以使用@SendTo("/topic/freeChat")注解显式指定发送的路径
    //@SendToUser("/topic/freeChat") 只发送给这个用户(如果这个客户订阅了这路径)
    public void userChat(ChatMsg chatMsg, MessageHeaders messageHeaders) throws Exception {
        //logger.info("收到消息：" + "[roomId="+ chatMsg.getRoomId() +"]" +chatMsg.getNickName() + " -> " + chatMsg.getChatContent());
        /*
        //恶意字符替换
        for (String k : xssMap.keySet()) {
            String v = xssMap.get(k);
            chatMsg.setNickName(chatMsg.getNickName().replaceAll(k, v));
            chatMsg.setChatContent(chatMsg.getChatContent().replaceAll(k, v));
        }
*/
        if (chatMsg.getNickName().equals("") || chatMsg.getChatContent().equals("")) {
            return;
        }


        // 找到需要发送的地址(客户端订阅地址)
        String dest = "/topic/" + chatMsg.getRoomId();
        // 获取缓存，并将用户最新的聊天记录存储到缓存中
        RoomContext roomContext = RoomContext.getInstance();
        Map<String, ChatRoom> roomMap = roomContext.getRoomMap();
        ChatRoom room = roomMap.get(chatMsg.getRoomId());
        chatMsg.setCurTime(new SimpleDateFormat().format(new Date()));

        boolean isForbidden = false;//是否屏蔽整条消息
        if (!isForbidden) {//如果没有被彻底屏蔽
            // 发送用户的聊天记录
            this.template.convertAndSend(dest, chatMsg);
            //消息存入聊天记录队列
            room.offer(chatMsg);
        } else {
            //直接放弃，不做广播
            log.debug("此条消息屏蔽");
        }


    }


    /**
     * 如果房间不存在，就创建房间。如果存在，就把用户直接加入房间，并返回已存在的聊天记录
     *
     * @param roomid
     * @param nickName
     * @param accountNo
     * @param welcome
     * @return
     */
    @SubscribeMapping("/initChat/{roomid}/{nickName}/{accountNo}/{welcome}")
    public ChatRoom<ChatMsg> initChatRoom(@DestinationVariable String roomid,
                                          @DestinationVariable String nickName,
                                          @DestinationVariable String accountNo,
                                          @DestinationVariable boolean welcome) {
        log.info("-------" + nickName + "进入聊天室------" + roomid);
        ChatRoom<ChatMsg> chatRoom = null;
        RoomContext roomContext = RoomContext.getInstance();
        Map<String, ChatRoom> roomMap = roomContext.getRoomMap();

        synchronized (roomMap) {
            // 返回历史聊天记录
            if (!roomMap.containsKey(roomid)) {// 从来没有人进入聊天空间
                chatRoom = new ChatRoom<ChatMsg>(maxChatHistory);
                roomMap.put(roomid, chatRoom);
            } else {//房间已有人进入，才返回历史消息
                String dest = "/topic/" + roomid;
                chatRoom = (ChatRoom<ChatMsg>) roomMap.get(roomid);

            }
            //当有人进入房间时，就把用户帐户加入Room
            chatRoom.addUser(accountNo);


        }
        return chatRoom;
    }


}
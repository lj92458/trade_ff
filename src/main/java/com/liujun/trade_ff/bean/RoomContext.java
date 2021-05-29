package com.liujun.trade_ff.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装所有聊天室，提供Map ，用roomid查找room
 * Created by Administrator on 2017/5/24.
 */
public class RoomContext {
    private static Logger logger = LoggerFactory.getLogger(com.liujun.trade_ff.bean.RoomContext.class);

    private static com.liujun.trade_ff.bean.RoomContext instance;
    private Map<String, com.liujun.trade_ff.bean.ChatRoom> roomMap;

    private RoomContext() {
        roomMap = new HashMap<String, com.liujun.trade_ff.bean.ChatRoom>();
    }

    /**
     * 单例构造方法
     */
    public synchronized static com.liujun.trade_ff.bean.RoomContext getInstance() {
        if (instance == null) {
            instance = new com.liujun.trade_ff.bean.RoomContext();
        }
        return instance;
    }

    public synchronized void putRoom(String roomId, com.liujun.trade_ff.bean.ChatRoom chatRoom) {
        if (chatRoom != null) {
            roomMap.put(roomId, chatRoom);
        }
    }

    public synchronized void removeRoom(String roomId) {
        roomMap.remove(roomId);
    }

    public Map<String, com.liujun.trade_ff.bean.ChatRoom> getRoomMap() {
        return this.roomMap;
    }

    /**
     * 根据roomId获取房间
     * @param roomId
     * @return
     */
    public com.liujun.trade_ff.bean.ChatRoom getRoomByRoomId(String roomId){
        if(roomMap.size() > 0){
            for (String m : roomMap.keySet()) {
                if (m.equals(roomId)) {
                 return  roomMap.get(m);

                }
            }
        }
      return null;
    }
}

package com.liujun.trade_ff.common.logback;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 等spring加载完成，从spring获取SimpMessagingTemplate类的实例
 */
public class WebSocketOutputStream extends OutputStream {
    //为什么要用静态？因为logback创建WebSocketOutputStream对象时，可能spring还没准备好,监听是后来才生效的
    private static SimpMessagingTemplate template;

    private List<Byte> byteList = new ArrayList<>(512);
    /**
     * websocket发送的目的地址，例如：/topic/rootLog
     */
    private String destination;

    public WebSocketOutputStream() {

    }

    @Override
    public void write(int b) throws IOException {

            if(b!='\n'&& b!='\r'){
                byteList.add((byte) b);
            }else if (b == '\n') {//如果是换行符，就发送消息
                flush();
            }

    }

    @Override
    public void flush() throws IOException {
        //只有当template被spring注入了值，才开始处理
        if (template != null) {
            byte[] arr = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                arr[i] = byteList.get(i);
            }
            if(arr.length>0) {
                sendMsg(new String(arr));
            }
            byteList.clear();
        }else{
            //System.out.println("logback的classLoader:"+WebSocketOutputStream.class.getClassLoader());
            //System.out.println("logback的this:"+this);
        }
    }

    private void sendMsg(String chatMsg) throws IOException {
        if (destination == null) {
            throw new IOException("webSocket发送广播消息的目的地址不能为空");
        } else {
            template.convertAndSend(destination, chatMsg);
        }
    }

    @Override
    public void close() throws IOException {
    }



    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public static SimpMessagingTemplate getTemplate() {
        return template;
    }

    public static void setTemplate(SimpMessagingTemplate template) {
        if(template!=null) {
            WebSocketOutputStream.template = template;
            //System.out.println("setTemplate的classloader:"+WebSocketOutputStream.class.getClassLoader());
        }else{
            //System.out.println("warn:template will be clearn");
        }
    }


}

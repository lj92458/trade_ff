package com.liujun.trade_ff.utils;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class HttpUtil {
    
    private static final String[] mobileUA = {"nokia",
            "sony",
            "ericsson",
            "mot",
            "samsung",
            "htc",
            "sgh",
            "lg",
            "sharp",
            "sie-",
            "philips",
            "panasonic",
            "alcatel",
            "lenovo",
            "iphone",
            "ipod",
            "blackberry",
            "meizu",
            "android",
            "netfront",
            "symbian",
            "ucweb",
            "windowsce",
            "palm",
            "operamini",
            "operamobi",
            "openwave",
            "nexusone",
            "cldc",
            "midp",
            "wap",
            "mobile",
            "huawei",
            "xiaomi"
    };
    
    /**
     * 从Request对象中获得客户端IP，处理了HTTP代理服务器和Nginx的反向代理截取了ip
     *
     * @param request 请求
     * @return ip
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.equals("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ip;
    }

    public static String getClientMac(String clientIp){
        String mac = null;
        try {
            Process p = Runtime.getRuntime().exec("arp -n");
            InputStreamReader ir = new InputStreamReader(p.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            //p.waitFor();
            boolean flag = true;
            String ipStr = "(" + clientIp + ")";
            while(flag) {
                String str = input.readLine();
                if (str != null) {
                    if (str.indexOf(ipStr) > 1) {
                        int temp = str.indexOf("at");
                        mac = str.substring(temp + 3, temp + 20);
                        break;
                    }
                } else
                    flag = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mac;
    }

    public static boolean isRequestFromMobile(HttpServletRequest request){
        String userAgent = request.getHeader("user-agent");
        if(StringUtil.isEmpty(userAgent)){
            return false;
        }
        userAgent = userAgent.toLowerCase();
        for(String ua : mobileUA){
            if(userAgent.contains(ua)){
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args){
        String ip = "127.0.0.1";
        String mac = getClientMac(ip);

        System.out.println(mac);
    }
}

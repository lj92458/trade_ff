package com.liujun.trade_ff.utils;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * Base64 工具类
 * Created by WuShaotong on 2016/8/12.
 */
public class Base64Util {
    // 加密
    public static String getBase64(String str) {
        byte[] b = null;
        String s = null;
        try {
            b = str.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (b != null) {
            s = new String(Base64.getEncoder().encode(b));
        }
        return s;
    }

    public static String getBase64(byte[] b) {
        String s = null;
        if (b != null) {
            s = new String(Base64.getEncoder().encode(b));
        }
        return s;
    }

    // 解密
    public static byte[] getFromBase64(String s) {
        byte[] b = null;
        String result = null;
        if (s != null) {
            try {
                b = Base64.getDecoder().decode(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return b;
    }
}
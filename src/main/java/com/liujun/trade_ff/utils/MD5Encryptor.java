package com.liujun.trade_ff.utils;

import java.security.MessageDigest;

public class MD5Encryptor {

    private static String key = "trade.liujun.com";          //自定义生成MD5加密字符串前的混合KEY
    private static String[] chars = new String[]{                  //要使用生成URL的字符
            "a", "b", "c", "d", "e", "f", "g", "h",
            "i", "j", "k", "l", "m", "n", "o", "p",
            "q", "r", "s", "t", "u", "v", "w", "x",
            "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D",
            "E", "F", "G", "H", "I", "J", "K", "L",
            "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
    };

	public static String byteArrayToHexString(byte[] bytes) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < bytes.length; n++) {
            stmp = (Integer.toHexString(bytes[n] & 0XFF));
            if (stmp.length() == 1)
                hs = hs + "0" + stmp;
            else
                hs = hs + stmp;
        }
        return hs;
    }

    public static String MD5Encode(String msg) {
        return MD5Encode(msg, null);
    }

    public static String MD5Encode(String msg, String charset) {
    	if(null == msg){
    		return null;
    	}
        String resultString = null;
        try {
            byte[] msgByte = null;
            if(null == charset || "".equals(charset.trim())){
            	msgByte = msg.getBytes();
            }else{
            	msgByte = msg.getBytes(charset);
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(msgByte));
        } catch (Exception ex) {
            return null;
        }
        return resultString.toUpperCase();
    }

    /**
     * 获取短MD5值
     * @param srcString 原字符串
     * @return 短MD5数组（长度为4）
     */
    public static String[] getShortMd5s(String srcString){
        String hex = MD5Encryptor.MD5Encode(key + srcString);
        int hexLen = hex.length();
        int subHexLen = hexLen / 8;
        String[] shortStr = new String[4];

        for (int i = 0; i < subHexLen; i++) {
            String outChars = "";
            int j = i + 1;
            String subHex = hex.substring(i * 8, j * 8);
            long idx = Long.valueOf("3FFFFFFF", 16) & Long.valueOf(subHex, 16);

            for (int k = 0; k < 6; k++) {
                int index = (int) (Long.valueOf("0000003D", 16) & idx);
                outChars += chars[index];
                idx = idx >> 5;
            }
            shortStr[i] = outChars;
        }
        return shortStr;
    }

    /**
     * 获取短MD5值
     * @param srcString 原字符串
     * @param index 获取到MD5的位次（0～3）
     * @return 第N个短MD5值
     */
    public static String getShortMd5(String srcString, int index){
        if(index <0 || index > 3){
            index = 0;
        }
        String[] md5Arr = getShortMd5s(srcString);
        if(null == md5Arr || md5Arr.length != 4){
            return null;
        }
        return md5Arr[index];
    }

    /**
     * 获取8字节的MD5值(中间16个字符)
     * @param msg 原始内容
     * @return 16个字符的MD5值
     */
    public static String get8ByteMd5(String msg){
        String fullMd5 = MD5Encode(msg);
        if(null != fullMd5 && fullMd5.length() == 32){
            return fullMd5.substring(8, 24);
        }else {
            return fullMd5;
        }
    }
}

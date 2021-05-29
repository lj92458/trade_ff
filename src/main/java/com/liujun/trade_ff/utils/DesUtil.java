package com.liujun.trade_ff.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

/**
 * DES 加解密
 * Created by WuShaotong on 2016/8/12.
 */
public class DesUtil {

    private static final String DEFAULT_PWD = "AK^&%@!G";

    //测试
    public static void main(String args[]) { //待加密内容
        String str = "测试内容"; //密码，长度要是8的倍数
        byte[] result = DesUtil.encrypt(str.getBytes());
        System.out.println("加密后：" + Base64Util.getBase64(result)); //直接将如上内容解密
        try {
            byte[] decryResult = DesUtil.decrypt(result);
            System.out.println("解密后：" + new String(decryResult));
        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }

    /**
     * 加密
     *
     * @param datasource byte[]
     * @return byte[]
     */
    public static byte[] encrypt(byte[] datasource) {
        return encrypt(datasource, DEFAULT_PWD);
    }

    /**
     * 加密
     *
     * @param datasource byte[]
     * @return byte[]
     */
    public static byte[] encrypt(String datasource) {
        byte[] byteArr = null;
        try {
            byteArr = datasource.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encrypt(byteArr, DEFAULT_PWD);
    }

    /**
     * 加密
     *
     * @param datasource byte[]
     * @param password   String
     * @return byte[]
     */
    public static byte[] encrypt(byte[] datasource, String password) {
        try {
            SecureRandom random = new SecureRandom();
            DESKeySpec desKey = new DESKeySpec(password.getBytes());//创建一个密匙工厂，然后用它把DESKeySpec转换成
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);//Cipher对象实际完成加密操作
            Cipher cipher = Cipher.getInstance("DES");//用密匙初始化Cipher对象
            cipher.init(Cipher.ENCRYPT_MODE, securekey, random);//现在，获取数据并加密//正式执行加密操作
            return cipher.doFinal(datasource);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密
     *
     * @param src      byte[]
     * @return byte[]
     */
    public static byte[] decrypt(byte[] src){// DES算法要求有一个可信任的随机数源
        return decrypt(src, DEFAULT_PWD);
    }

    /**
     * 解密
     *
     * @param src      byte[]
     * @param password String
     * @return byte[]
     */
    public static byte[] decrypt(byte[] src, String password){// DES算法要求有一个可信任的随机数源
        try {
            SecureRandom random = new SecureRandom();// 创建一个DESKeySpec对象
            DESKeySpec desKey = new DESKeySpec(password.getBytes());// 创建一个密匙工厂
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");// 将DESKeySpec对象转换成SecretKey对象
            SecretKey securekey = keyFactory.generateSecret(desKey);// Cipher对象实际完成解密操作
            Cipher cipher = Cipher.getInstance("DES");// 用密匙初始化Cipher对象
            cipher.init(Cipher.DECRYPT_MODE, securekey, random);// 真正开始解密操作
            return cipher.doFinal(src);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

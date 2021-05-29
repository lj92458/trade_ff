package com.liujun.trade_ff.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公共工具类
 * Created by WuShaotong on 2016/8/9.
 */
public class CommonUtil {
    private static final int SALT_LENGTH = 64;      //加密盐长度
    private static final char[] charArr = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z','a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
            'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '`','~','!','@','#','$','%','^','&','*','(',')','_',
            '-','+','=','[',']','{','}',':',';','"','\'','<',',',
            '>','.','?','/','|','\\'
    };
    private static final SimpleDateFormat SDF1 = new SimpleDateFormat("yyMMddHHmm");
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("yyMMdd");
    private static final SimpleDateFormat SDF3 = new SimpleDateFormat("yyyyMMdd");


    /**
     * 生成随机加密盐
     * @return 加密盐
     */
    public static String generateRandomSalt(){
        StringBuffer salt = new StringBuffer();
        for(int i=0;i<SALT_LENGTH;i++){
            int randomIndex = (int)(Math.floor(Math.random() * charArr.length));
            salt.append(charArr[randomIndex]);
        }
        return salt.toString();
    }

    /**
     * 生成随机短信验证码
     * 规则：6位数字
     * @return 短信验证码
     */
    public static String getRandomSmsVerifyCode(){
        int num = (int)Math.floor(Math.random() * 999999);
        return  String.format("%06d", num);
    }

    /**
     * 生成随机邮箱验证码
     * 规则：32位大写字母+数字
     * @return 邮箱验证码
     */
    public static String getRandomEmailVerifyCode(){
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<32;i++){
            int num = (int)Math.floor(Math.random() * 36);
            if(num < 10){
                sb.append((char)(num + 48));
            }else{
                sb.append((char)(num + 55));
            }
        }
        return  sb.toString();
    }

    /**
     * 获取字符串的倒叙字符串
     * @param src 源字符串
     * @return 倒叙后的字符串
     */
    public static String getStringDesc(String src){
        if(null == src){
            return null;
        }
        StringBuffer sb = new StringBuffer();
        char[] charArr = src.toCharArray();
        for(int i = charArr.length - 1; i>=0; i--){
            sb.append(charArr[i]);
        }
        return sb.toString();
    }

    /**
     * 生成随机的UserAccount
     * @return 用户账号
     */
    public static String generateRandomUserAccount(){
        int startYear = 2016;
        int baseYear = 1970;
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String curTimeStr = sdf.format(new Date());
        String transTimeStr = curTimeStr.replace(year+"", (year - startYear + baseYear)+"");
        StringBuffer s = new StringBuffer();
        s.append("u");
        try {
            Date transDate = sdf.parse(transTimeStr);
            s.append(String.format("%d", transDate.getTime()));
            int randomInt = (int)Math.floor(Math.random() * 999);
            s.append(String.format("%03d", randomInt));
        } catch (ParseException e) {
            return null;
        }
        return s.toString();
    }

    /**
     * 校验短信验证码格式
     * @param smsVerifyCode 短信验证码
     * @return 是否正确
     */
    public static boolean isSmsVerifyCode(String smsVerifyCode){
        if(null == smsVerifyCode){
            return false;
        }
        String regExp = "^[0-9]{6}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(smsVerifyCode);
        return m.find();
    }
    /**
     * 校验图片验证码格式
     * @param imgVerifyCode 图片验证码
     * @return 是否正确
     */
    public static boolean isImgVerifyCode(String imgVerifyCode){
        if(null == imgVerifyCode){
            return false;
        }
        String regExp = "^[0-9a-zA-Z]{4}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(imgVerifyCode);
        return m.find();
    }

    /**
     * 验证是否是手机号
     * @param mobile 手机号
     * @return 是否是手机号
     */
    public static boolean isMobile(String mobile){
        if(null == mobile){
            return false;
        }
        String regExp = "^[1][0-9]{10}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(mobile);
        return m.find();
    }

    /**
     * 验证是否是电话号
     * @param phone 电话号
     * @return 是否是电话号
     */
    public static boolean isPhoneNum(String phone){
        if(null == phone){
            return false;
        }
        String regExp = "^([0][0-9]{2,3}[\\-])?[1-9][0-9]{6,7}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(phone);
        return m.find();
    }

    public static boolean isPostCode(String postCode){
        if(null == postCode){
            return false;
        }
        String regExp = "^[0-9]{6}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(postCode);
        return m.find();
    }

    /**
     * 是否是邮箱
     * @param email 邮箱
     * @return 是否是邮箱
     */
    public static boolean isEmail(String email){
        if(null == email){
            return false;
        }
        String regExp = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]?@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(email);
        return m.find();
    }

    /**
     * 验证密码是否合法
     * 规则1：不能全数字
     * 规则2：不能全字母
     * 规则3：数字、字母或特殊符号的组合
     * @param password 密码
     * @return 是否合法
     */
    public static boolean isLegalPassword(String password){
        String regExp1 = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z\\|~`\\!@#\\$%\\^\\&\\*\\(\\);:'\"\\[\\]\\{\\}\\,\\<\\.\\>/\\?\\-_\\+=\\\\]{8,30}$";
        Pattern p1 = Pattern.compile(regExp1);
        Matcher m1 = p1.matcher(password);
        if(m1.find()){
            return true;
        }
        String regExp2 = "^[0-9A-Za-z]{32}$";       //可能为密码的MD5
        Pattern p2 = Pattern.compile(regExp2);
        Matcher m2 = p2.matcher(password);
        return m2.find();
    }

    /**
     * 是否是快递单号
     * @param expressOrder 快递单号
     * @return 是否是快递单号
     */
    public static boolean isLegalExpressOrder(String expressOrder){
        if(null == expressOrder){
            return false;
        }
        String regExp = "^[a-z0-9A-Z]{5,32}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(expressOrder);
        return m.find();
    }

    /**
     * 隐藏手机号
     * @param mobile 手机号
     * @return 隐藏的手机号
     */
    public static String hideMobile(String mobile){
        if(!isMobile(mobile)){
            return null;
        }
        return mobile.substring(0,3) + "*******" + mobile.substring(10,11);
    }

    /**
     * 隐藏邮箱账号
     * @param email 邮箱
     * @return 隐藏的邮箱
     */
    public static String hideEmail(String email){
        if(!isEmail(email)){
            return null;
        }
        String[] tempArr1 = email.split("@");
        String[] tempArr2 = tempArr1[1].split("[.]");
        String part1 = tempArr1[0];
        String part2 = tempArr2[0];
        String part3 = tempArr2[1];

        if(part1.length() > 2){
            part1 = part1.substring(0,2) + String.format("%" + (part1.length() - 2) +"S", "");
            part1 = part1.replaceAll("\\s", "*");
        }
        if(part2.length() > 1){
            part2 = part2.substring(0,1) + String.format("%" + (part2.length() - 1) +"S", "");
            part2 = part2.replaceAll("\\s", "*");
        }

        return part1 + "@" + part2 + "." + part3;
    }

    /**
     * 隐藏真实姓名
     * @param realName 真实姓名
     * @return 隐藏的真实姓名
     */
    public static String hideRealName(String realName){
        if(null == realName || "".equals(realName.trim()) || realName.trim().length() == 1){
            return realName;
        }
        realName = realName.trim();

        StringBuffer sb = new StringBuffer(realName.substring(0, 1));
        for(int i=1;i<realName.length();i++){
            sb.append("*");
        }
        return sb.toString();
    }

    /**
     * 隐藏证件号
     * @param certNo 证件号
     * @return 隐藏的证件号
     */
    public static String hideCertNo(String certNo){
        if(null == certNo || certNo.trim().length() <= 5){
            return certNo;
        }
        certNo = certNo.trim();

        StringBuffer sb = new StringBuffer(certNo.substring(0, 3));
        for(int i=3;i<certNo.length();i++){
            sb.append("*");
        }
        sb.append(certNo.substring(certNo.length() - 2, certNo.length()));
        return sb.toString();
    }

    /**
     * 生成订单ID<br/>
     * 格式：161231235900000199
     * @param seqValue 序列值
     * @param curTime 序列取值时间（数据库时间）
     * @return 订单ID
     */
    public static String generateOrderId(long seqValue, Date curTime){
        StringBuffer orderId = new StringBuffer(SDF1.format(curTime));      //日期 + 时间
        orderId.append(String.format("%06d", seqValue));                    //序列
        orderId.append(getRandomNumber(2));                                 //随机数
        return orderId.toString();
    }
    /**
     * 生成退款订单ID<br/>
     * 格式：T161231000199
     * @param seqValue 序列值
     * @param curTime 序列取值时间（数据库时间）
     * @return 订单ID
     */
    public static String generateRefundOrderId(long seqValue, Date curTime){
        StringBuffer refundOrderId = new StringBuffer("T");
        refundOrderId.append(SDF2.format(curTime));      //日期 + 时间
        refundOrderId.append(String.format("%04d", seqValue));                    //序列
        refundOrderId.append(getRandomNumber(2));                                 //随机数
        return refundOrderId.toString();
    }
    /**
     * 生成换货订单ID<br/>
     * 格式：H161231000199
     * @param seqValue 序列值
     * @param curTime 序列取值时间（数据库时间）
     * @return 订单ID
     */
    public static String generateReplaceOrderId(long seqValue, Date curTime){
        StringBuffer replaceOrderId = new StringBuffer("H");
        replaceOrderId.append(SDF2.format(curTime));      //日期 + 时间
        replaceOrderId.append(String.format("%04d", seqValue));                    //序列
        replaceOrderId.append(getRandomNumber(2));                                 //随机数
        return replaceOrderId.toString();
    }

    /**
     * 生成交易流水ID<br/>
     * 格式：1612312359000000019999
     * @param seqValue 序列值
     * @param curTime 序列取值时间（数据库时间）
     * @return 交易流水ID
     */
    public static String generateTransId(long seqValue, Date curTime){
        StringBuffer orderId = new StringBuffer(SDF1.format(curTime));      //日期 + 时间
        orderId.append(String.format("%08d", seqValue));                    //序列
        orderId.append(getRandomNumber(4));                                 //随机数
        return orderId.toString();
    }

    /**
     * 计算清算日期（下月清/T+N清/下周清）
     * @param baseTime 基于某个时间（if null 当前时间）
     * @param settleType 清算类型（month / T / week
     * @param settleDate 清算日（间隔日）1～28 / 1～ / 1～5
     * @param weekStartWithSunday 每周第一天是否周日
     * @return 清算日期（若参数不合法 返回null）
     */
    public static String getSettlementDate(Date baseTime, String settleType, int settleDate, boolean weekStartWithSunday){
        if(null == baseTime){
            baseTime = new Date();
        }
        String settlementDate = null;
        Calendar c = Calendar.getInstance();
        c.setTime(baseTime);
        if("month".equals(settleType) && settleDate >= 1 && settleDate <= 28){
            c.set(Calendar.DAY_OF_MONTH, settleDate);
            c.add(Calendar.MONTH, 1);
            settlementDate = SDF3.format(c.getTime());
        }else if("T".equals(settleType) && settleDate >= 1){
            c.add(Calendar.DATE, settleDate);
            settlementDate = SDF3.format(c.getTime());
        }else if("week".equals(settleType) && settleDate >= 1 && settleDate <= 5){
            boolean isSunday = c.get(Calendar.DAY_OF_WEEK) == 1;
            if(isSunday && !weekStartWithSunday && settleDate > 0){
                //如果当天是周日，但周日不是每周开始日期，则取本周
            }else{
                c.add(Calendar.WEEK_OF_YEAR, 1);
            }
            c.set(Calendar.DAY_OF_WEEK, settleDate + 1);
            settlementDate = SDF3.format(c.getTime());
        }
        return settlementDate;
    }

    /**
     * 私有方法：获取指定数量的数字 拼接成字符串
     * @param numCount 数字个数
     * @return
     */
    private static String getRandomNumber(int numCount){
        if(numCount <= 0){
            return "";
        }
        StringBuffer numStr = new StringBuffer();
        for(int i=0;i<numCount;i++){
            numStr.append((int)(Math.floor(Math.random() * 10)));
        }
        return numStr.toString();
    }

    /**
     * 私有方法：获取指定数量的数字/大写字母 拼接成字符串
     * @param numCount 字符个数
     * @return
     */
    public static String getRandomChar(int numCount){
        if(numCount <= 0){
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<numCount;i++){
            int num = (int)Math.floor(Math.random() * 36);
            if(num < 10){
                sb.append((char)(num + 48));
            }else{
                sb.append((char)(num + 55));
            }
        }
        return  sb.toString();
    }

    /**
     * test main
     */
    public static void main(String[] args){
        SimpleDateFormat testSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date d = testSdf.parse("2016-12-26 23:56:48");
            String s = getSettlementDate(d, "week", 1, false);
            System.out.println(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

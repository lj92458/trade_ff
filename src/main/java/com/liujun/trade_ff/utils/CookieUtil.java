package com.liujun.trade_ff.utils;

import com.liujun.trade_ff.bean.AutoSigninBean;

import javax.servlet.http.Cookie;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Cookies 相关工具类
 * Created by WuShaotong on 2016/12/28.
 */
public class CookieUtil {
    private static String MIX_STRING = "auto.signin@web.cosmetics@www.lifegolds.com@HuaXun";            //签名混淆字符串
    private static String RANDOM_KEY_CK = "__signin_randomKey__";
    private static String ENCODE_DATA_CK = "__signin_encodeData__";
    private static String SIGN_CK = "__signin_sign__";

    private static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成自动登录所用cookie
     * @param userAccount 登录账号
     * @param userAuthString 用户身份验证信息（不是也不可以是用户密码，可以与浏览器UA有关系）
     * @param validDate cookie有效天数
     * @return 生成的cookies
     */
    public static List<Cookie> generateAutoSigninCookies(String userAccount, String userAuthString, int validDate, String requestContextPath){
        try {
            AutoSigninBean autoSigninBean = new AutoSigninBean();
            autoSigninBean.setUserAccount(userAccount);
            autoSigninBean.setUserAuth(MD5Encryptor.MD5Encode(userAuthString));
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, validDate);
            autoSigninBean.setExpireTime(SDF.format(c.getTime()));

            String json = JsonUtil.beanToJson(autoSigninBean);
            String sign = MD5Encryptor.MD5Encode(MIX_STRING + json);                        //for cookie "__signin_sign__"
            String jsonEncodeStep1 = StringUtil.bytesToHexString(DesUtil.encrypt(json));
            String randomKey = MD5Encryptor.MD5Encode(MIX_STRING + Math.random());          //for cookie "__signin_randomKey__"
            String randomDesKey = "HX" + MD5Encryptor.getShortMd5(randomKey, 0);
            String jsonEncodeStep2 = StringUtil.bytesToHexString(DesUtil.encrypt(jsonEncodeStep1.getBytes(), randomDesKey));        //for cookie "__signin_encodeData__"

            List<Cookie> cookieList = new ArrayList<Cookie>();
            Cookie randomKeyCookie = new Cookie(RANDOM_KEY_CK, randomKey);
            Cookie encodeDataCookie = new Cookie(ENCODE_DATA_CK, jsonEncodeStep2);
            Cookie signCookie = new Cookie(SIGN_CK, sign);
            //设置cookie path
            randomKeyCookie.setPath("/" + requestContextPath);
            encodeDataCookie.setPath("/" + requestContextPath);
            signCookie.setPath("/" + requestContextPath);
            //设置cookie有效期
            int cookieMaxAge = validDate * 24 * 60 * 60;       //cookie有效期
            randomKeyCookie.setMaxAge(cookieMaxAge);
            encodeDataCookie.setMaxAge(cookieMaxAge);
            signCookie.setMaxAge(cookieMaxAge);

            cookieList.add(randomKeyCookie);
            cookieList.add(encodeDataCookie);
            cookieList.add(signCookie);
            return cookieList;
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 验证自动登录的cookies
     * @param userAuthString 用户身份验证信息（不是也不可以是用户密码，可以与浏览器UA有关系） 与放入cookie的规则一致
     * @return 用户账号（若验证失败返回为空）
     */
    public static String validAutoSigninCookies(Cookie[] cookies, String userAuthString){
        if(null == cookies || cookies.length < 3 || null == userAuthString){
            return null;
        }
        String randomKey = null;
        String encodeData = null;
        String sign = null;
        for(Cookie cur : cookies){
            if(RANDOM_KEY_CK.equals(cur.getName())){
                randomKey = cur.getValue();
            }else if(ENCODE_DATA_CK.equals(cur.getName())){
                encodeData = cur.getValue();
            }else if(SIGN_CK.equals(cur.getName())){
                sign = cur.getValue();
            }
        }
        if(null == randomKey || null == encodeData || null == sign){
            return null;
        }
        try {
            String randomDesKey = "HX" + MD5Encryptor.getShortMd5(randomKey, 0);
            String decodeStep1 = new String(DesUtil.decrypt(StringUtil.hexStringToBytes(encodeData), randomDesKey));
            String json = new String(DesUtil.decrypt(StringUtil.hexStringToBytes(decodeStep1)));     //json数据
            //校验签名
            String signLocal = MD5Encryptor.MD5Encode(MIX_STRING + json);
            if(!sign.equals(signLocal)){
                //签名未校验通过
                return null;
            }
            AutoSigninBean autoSigninBean = JsonUtil.jsonToBean(json, AutoSigninBean.class);
            String userAccount = autoSigninBean.getUserAccount();
            String userAuth = autoSigninBean.getUserAuth();
            String expireTimeString = autoSigninBean.getExpireTime();
            if(null == userAccount || null == userAuth || null == expireTimeString){
                //数据有空值
                return null;
            }
            //校验 用户身份验证
            String userAuthLocal = MD5Encryptor.MD5Encode(userAuthString);
            if(!userAuth.equalsIgnoreCase(userAuthLocal)){
                //用户权限校验未通过
                return null;
            }
            //校验时效
            Date expireTime = SDF.parse(expireTimeString);
            Date curDate = new Date();
            if(curDate.getTime() >= expireTime.getTime()){
                //超过自动有效期
                return null;
            }
            return userAccount;
        }catch (Exception e){
            //异常
            return null;
        }
    }
}

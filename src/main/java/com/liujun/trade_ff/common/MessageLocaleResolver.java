package com.liujun.trade_ff.common;

import com.liujun.trade_ff.utils.HttpUtil;
import com.liujun.trade_ff.utils.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AbstractLocaleResolver;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Locale;
public class MessageLocaleResolver extends AbstractLocaleResolver{

	private static final String LOCAL_PARAM_NAME = "language";
	private static final String SKIN_PARAM_NAME = "skin";			//更换皮肤的请求参数 default-ui:电脑端样式/mobile-ui:移动端样式
	private static final String SKIN_DEFAULT = "default-ui";
	private static final String SKIN_MOBILE = "wap-ui";
	private static final String SKIN_SESSION_KEY = "PAGE_SKIN";
	private static final String LAST_ENTER_URL_PARAM_NAME = "LAST_ENTER_URL";

	/**
	 * 处理逻辑：
	 * 一、优先URL传入的参数
	 * 1、若有参数，设置或更新cookies + 更新session，并指定语言为此值；
	 * 2、若无参数 执行步骤“二”；
	 * 二、从session中获取
	 * 1、若session中有，指定语言为此值；
	 * 2、若session中没有，执行步骤“三”；
	 * 三、从cookies中获取
	 * 1、若cookies中有，更新session，并指定语言为此值；
	 * 2、若cookies中没有 执行步骤“四”；
	 * 四、从Accept Head获取
	 * 1、若有参数且能匹配，设置或更新cookies + 更新session，并指定语言为此值；
	 * 2、若没有或不匹配，指定为zh_CN，设置或更新cookies + 更新session，并指定语言为zh_CN；
	 */
	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		HttpSession session = request.getSession();
		Locale locale = null;

		enterUrlCache(request);		//缓存进入时的url

		//一、URL Locale
		try{
			String urlLocalStr = request.getParameter(LOCAL_PARAM_NAME);
			locale = StringUtils.parseLocaleString(urlLocalStr);
		}catch(Exception e){
			locale = null;
		}
		if(null != locale){
			session.setAttribute(LOCAL_PARAM_NAME,locale);
			resetPageSkin(request, locale);	//设置页面对应皮肤
			return locale;
		}
		//二、Session Locale
		try{
			locale = (Locale)session.getAttribute(LOCAL_PARAM_NAME);
		}catch(Exception e){
			locale = null;
		}
		if(null != locale){
			resetPageSkin(request, locale);	//设置页面对应皮肤
			return locale;
		}
		//三、Cookie Locale
		try{
			Cookie cookie = null;
			Cookie[] cookies = null;
			cookies = request.getCookies();
			if(cookies != null){
				for (int i = 0; i < cookies.length; i++){
					cookie = cookies[i];
					if(LOCAL_PARAM_NAME.equals(cookie.getName())){
						locale = StringUtils.parseLocaleString(cookie.getValue());
						break;
					}else{
						cookie = null;
					}
				}
			}
		}catch(Exception e){
			locale = null;
		}
		if(null != locale){
			session.setAttribute(LOCAL_PARAM_NAME,locale);
			resetPageSkin(request, locale);	//设置页面对应皮肤
			return locale;
		}
		//四、Accept head Locale
		locale = request.getLocale();
		session.setAttribute(LOCAL_PARAM_NAME,locale);
		resetPageSkin(request, locale);	//设置页面对应皮肤
		return locale;
	}

	@Override
	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		try{
			HttpSession session = request.getSession();
			//过滤 locale
			if(null == locale){
				locale = StringUtils.parseLocaleString("zh_CN");
			}else if("zh_CN".equals(locale.toString())){
			}else if("en_US".equals(locale.toString())){
			}else{
				locale = StringUtils.parseLocaleString("zh_CN");
			}
			//设置session
			session.setAttribute(LOCAL_PARAM_NAME,locale);
			//设置cookies
			Cookie localCookie = new Cookie(LOCAL_PARAM_NAME, locale.toString());
			localCookie.setMaxAge(60 * 60 * 24 * 7);// 设置cookie过期时间为1周 
			response.addCookie(localCookie);// 在响应头部添加cookie
		}catch(Exception e){
			//设置locale异常
		}
	}

	/**
	 * 设置页面对应的皮肤
	 * @param request
	 * @param locale
	 */
	private void resetPageSkin(HttpServletRequest request, Locale locale){

		//改动 （适应移动版）
		HttpSession session = request.getSession();
		String skinName = request.getParameter(SKIN_PARAM_NAME);
		String isForceDefaultSkin = (String) session.getAttribute("isForceDefaultSkin");		//是否强制使用默认样式 y/n
		if(SKIN_MOBILE.equalsIgnoreCase(skinName)){
			//移动端请求
			session.setAttribute(SKIN_SESSION_KEY, "/skins/mobile/");
			session.removeAttribute("isForceDefaultSkin");
		}else if(SKIN_DEFAULT.equalsIgnoreCase(skinName)){
			//电脑端请求
			session.setAttribute(SKIN_SESSION_KEY,"");
			session.setAttribute("isForceDefaultSkin", "y");
		}else if((null == isForceDefaultSkin || "n".equalsIgnoreCase(isForceDefaultSkin)) && HttpUtil.isRequestFromMobile(request)){
			session.setAttribute(SKIN_SESSION_KEY, "/skins/mobile/");
		}
		//记录来源标识（例如：wxpublic）
		String fromFlag = request.getParameter("fromFlag");
		if(!StringUtil.isEmpty(fromFlag)){
			session.setAttribute("fromFlag", fromFlag);
		}
	}

	private void enterUrlCache(HttpServletRequest request){
		String enterUrl = request.getRequestURI();
		try{
			enterUrl = enterUrl.split("[?]")[0];
			if(!enterUrl.endsWith(".html")){
				return;
			}
		}catch(Exception e){
			enterUrl = request.getContextPath() + "/index.html";
		}

		request.setAttribute(LAST_ENTER_URL_PARAM_NAME, enterUrl);
	}
}

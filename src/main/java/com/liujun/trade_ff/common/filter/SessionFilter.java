package com.liujun.trade_ff.common.filter;


import com.liujun.trade_ff.model.UserAccount;
import com.liujun.trade_ff.service.UserService;
import com.liujun.trade_ff.utils.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Session 过滤器
 * Created by WuShaotong on 2016/8/15.
 */
public class SessionFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SessionFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        //设置cookies
        Cookie localCookie = new Cookie("_client_check_flag_", "true");         //设置cookie 由客户端检测 是否启用cookie
        localCookie.setMaxAge(60 * 60);// 设置cookie过期时间为1小时
        httpServletResponse.addCookie(localCookie);// 在响应头部添加cookie

        String servletPath = httpServletRequest.getServletPath();

        if (servletPath.equals("/")) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }


        // 不过滤的uri
        String[] notFilter = new String[]{
                "/menu_.*",
                "/webchat/.*", "/app/.*", "/topic/.*", "/queue/.*", ".*/cancelCloseRoom", ".*/closeChatRoom", ".*/chat_customer.html",
                "/engine/queryDiffPrice.*", "/user/signin.html.*", "/user/register.html.*", "/user/checkIsLogin.*", "/index.html.*", "/callback/.*", "/js/.*", "/css/.*", "/images/.*",
                "/robots.txt", "/sitemap.xml", "/skins/.*", "/proxy.html.*", "/proxy_close.html.*", "/common/imgVerifyCode.html.*"};
        for (String not : notFilter) {
            if (servletPath.matches(not)) {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
                return;
            }
        }
        HttpSession session = httpServletRequest.getSession();
        UserAccount userAccount = null;
        try {
            userAccount = (UserAccount) session.getAttribute("userAccount");
        } catch (Exception e) {
            log.info("无登录信息");
        }
        //如果没有登录，尝试读取cookie，并自动登录
        if (null == userAccount) {
            //自动登录
            String userAuthString = httpServletRequest.getHeader("user-agent");
            if (null != userAuthString) {
                userAuthString = userAuthString.replaceAll("[()]", "");
            }
            String account = CookieUtil.validAutoSigninCookies(httpServletRequest.getCookies(), userAuthString);
            if (null != account) {//根据cookie中的用户信息，读取数据库中的用户信息
                WebApplicationContext webApplicationContext = ContextLoader.getCurrentWebApplicationContext();
                UserService userService = webApplicationContext.getBean(UserService.class);
                userAccount = userService.getUserByAccount(account);
                if (null != userAccount) {
                    //正常状态
                    session.setAttribute("userAccount", userAccount);
                }
            }
        }


        //如果还是没有登录
        if (null == userAccount) {

            //1对ajax请求特殊处理
            if (httpServletRequest.getHeader("x-requested-with") != null
                    && "XMLHttpRequest".equalsIgnoreCase(httpServletRequest.getHeader("x-requested-with"))) {
                log.info("ajax请求,没有登录，被拦截");

                //向http头添加 状态 sessionstatus
                httpServletResponse.setHeader("sessionstatus", "timeout");
                httpServletResponse.setStatus(403);
                //向http头添加登录的url
                httpServletResponse.addHeader("loginPath", httpServletRequest.getContextPath() + "/user/signin.html");
                httpServletResponse.setContentType("application/json; charset=utf-8");
                PrintWriter writer = httpServletResponse.getWriter();
                Map<String, String> map = new HashMap<>();
                map.put("retMsg", "请重新登录");
                map.put("retCode", "0000");
                writer.write(map.toString());
            } else {
                log.info("一般请求,没有登录，被拦截：" + servletPath);
                httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/user/signin.html");

            }
            return;
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}

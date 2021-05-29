package com.liujun.trade_ff.common.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 防跨站攻击 过滤器
 * Created by WuShaotong on 2016/9/22.
 */
public class XSSFilter implements Filter {
    // XSS处理Map
    private static Map<String,String> xssMap = new LinkedHashMap<String,String>();
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 含有脚本： script
        xssMap.put("[s|S][c|C][r|R][i|C][p|P][t|T]", "");
        // 含有脚本 javascript
        xssMap.put("[\\\"\\\'][\\s]*[j|J][a|A][v|V][a|A][s|S][c|C][r|R][i|I][p|P][t|T]:(.*)[\\\"\\\']", "");
        // 含有函数： eval
        xssMap.put("[e|E][v|V][a|A][l|L]\\((.*)\\)", "");
        // 含有符号 <
        xssMap.put("<", "");
        // 含有符号 >
        xssMap.put(">", "");
        // 含有符号 (
        xssMap.put("\\(", "");
        // 含有符号 )
        xssMap.put("\\)", "");
        // 含有符号 '
        xssMap.put("'", "");
        // 含有符号 "
        xssMap.put("\"", "");
        // 含有符号 \
        xssMap.put("\\\\", "");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 强制类型转换 HttpServletRequest
        HttpServletRequest httpReq = (HttpServletRequest)request;
        // 构造HttpRequestWrapper对象处理XSS
        HttpRequestWrapper httpReqWarp = new HttpRequestWrapper(httpReq,xssMap);
        //
        chain.doFilter(httpReqWarp, response);
    }

    @Override
    public void destroy() {

    }
}

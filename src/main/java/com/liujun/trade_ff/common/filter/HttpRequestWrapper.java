package com.liujun.trade_ff.common.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by WuShaotong on 2016/9/22.
 */
public final class HttpRequestWrapper extends HttpServletRequestWrapper {

    private Map<String, String> xssMap;

    public HttpRequestWrapper(HttpServletRequest request) {
        super(request);
    }
    public HttpRequestWrapper(HttpServletRequest request,
                              Map<String, String> xssMap) {
        super(request);
        this.xssMap = xssMap;
    }

    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        int count = values.length;
        // 遍历每一个参数，检查是否含有
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = cleanXSS(values[i]);
        }
        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        if (value == null) {
            return null;
        }
        return cleanXSS(value);
    }

    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (value == null)
            return null;
        return cleanXSS(value);

    }

    /**
     * 清除恶意的XSS脚本
     *
     * @param value
     * @return
     */
    private String cleanXSS(String value) {
        if(null == value){
            return value;
        }
        Set<String> keySet = xssMap.keySet();
        for(String key : keySet){
            String v = xssMap.get(key);
            value = value.replaceAll(key,v);
        }
        return value.trim();
    }
}

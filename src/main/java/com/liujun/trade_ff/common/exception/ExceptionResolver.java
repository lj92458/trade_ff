package com.liujun.trade_ff.common.exception;

import com.liujun.trade_ff.utils.JsonUtil;
import com.liujun.trade_ff.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 异常处理类
 * Created by WuShaotong on 2017/1/9.
 */
public class ExceptionResolver implements HandlerExceptionResolver {
    private static Logger log = LoggerFactory.getLogger(ExceptionResolver.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object o, Exception exception) {

        // 判断是否ajax请求
        if (request.getHeader("x-requested-with") != null
                && "XMLHttpRequest".equalsIgnoreCase(request.getHeader("x-requested-with"))) {
            // 如果是ajax请求，JSON格式返回
            try {
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter writer = response.getWriter();
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("retCode", "0001");
                map.put("retMsg", "抱歉，服务器出错了，请稍后再试...");
                if (exception instanceof BizException) {
                    BizException bizException = (BizException) exception;
                    if (!StringUtil.isEmpty(bizException.getErrCode())) {
                        map.put("retCode", ((BizException) exception).getErrCode());
                        map.put("retMsg", ((BizException) exception).getErrMsg());
                    }
                }
                log.error(exception.getMessage(),exception);
                writer.write(JsonUtil.mapToJson(map, null, false));
                writer.flush();
                writer.close();
            } catch (Exception e) {
                log.error("异常处理类 resolveException，出错：", e);
            }
        } else {
            // 如果不是ajax，JSP格式返回
            // 为安全起见，只有业务异常我们对前端可见，否则否则统一归为系统异常
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("errCode", "0001");
            map.put("errMsg", "抱歉，服务器出错了，请稍后再试...");
            if (exception instanceof BizException) {
                BizException bizException = (BizException) exception;
                if (!StringUtil.isEmpty(bizException.getErrCode())) {
                    map.put("errCode", ((BizException) exception).getErrCode());
                    map.put("errMsg", ((BizException) exception).getErrMsg());
                }
            }
            log.error("异常处理类，展示异常：", exception);
            return new ModelAndView("error/error_50x", map);

        }


        return null;
    }
}

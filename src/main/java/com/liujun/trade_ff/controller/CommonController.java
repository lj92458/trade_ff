package com.liujun.trade_ff.controller;


import com.liujun.trade_ff.utils.VerifyCodeUtil;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@Api(hidden = true)
public class CommonController {
    private static Logger log = LoggerFactory.getLogger(CommonController.class);



    /**
     * 获取图形验证码
     */
    @RequestMapping(value = "/common/imgVerifyCode.html", method= RequestMethod.GET)
    public void getImgVerifyCode(HttpServletRequest request, HttpServletResponse response, Model model) {

        // 禁止图像缓存。
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");

        HttpSession session = request.getSession();
        ServletOutputStream sos = null;
        try {
            sos = response.getOutputStream();
            String verifyCode = VerifyCodeUtil.generateVerifyCode(sos);
            // 将四位数字的验证码保存到Session中。
            session.setAttribute("imgVerifyCode", verifyCode);
        } catch (Exception e) {
            session.setAttribute("imgVerifyCode", null);
            log.error("把验证码写入流，异常：", e);
        }finally {
            try {
                if(null != sos) {
                    sos.close();
                }
            }catch (Exception e){
                log.debug("关闭流异常");
            }
        }
    }



}

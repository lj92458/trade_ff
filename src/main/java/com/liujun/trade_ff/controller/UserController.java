package com.liujun.trade_ff.controller;

import com.liujun.trade_ff.model.UserAccount;
import com.liujun.trade_ff.service.UserService;
import com.liujun.trade_ff.utils.CommonUtil;
import com.liujun.trade_ff.utils.CookieUtil;
import com.liujun.trade_ff.utils.StringUtil;
import com.liujun.trade_ff.vo.SignInVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class UserController {
    private static Logger log = LoggerFactory.getLogger(UserController.class);

    private static final int AUTO_SIGN_VALID_DATE = 30;     //自动登录有效天数（依赖cookies）

    @Autowired
    UserService userService;

    /**
     * 注册
     */
    @RequestMapping(value = "/user/register.html", method = RequestMethod.GET)
    public String toRegister(HttpServletRequest request, Model model) {
        return "user/register";
    }


    /**
     * 登录
     */
    @RequestMapping(value = "/user/signin.html", method = RequestMethod.GET)
    public String toSignIn(HttpServletRequest request, Model model) {
        return "user/signin";
    }

    /**
     * 登录 提交
     */
    @RequestMapping(value = "/user/signin.html", method = RequestMethod.POST)
    public String signInSubmit(HttpServletRequest request, HttpServletResponse response, Model model, @ModelAttribute("signInObj") SignInVO signInObj) {
        //model.addAttribute("signInObj", signInObj);
        String account = signInObj.getAccount();
        String password = signInObj.getPassword();
        String verifyCode = signInObj.getVerifyCode();
        Boolean isAutoSign = signInObj.getIsAutoSign();

        HttpSession session = request.getSession();

        boolean isParamValid = true;
        if (StringUtil.isEmpty(account)) {
            isParamValid = false;
            signInObj.setErrMsgAccount("账号不允许为空");
        }
        if (!CommonUtil.isMobile(account) && !CommonUtil.isEmail(account)) {
            isParamValid = false;
            signInObj.setErrMsgAccount("账号是手机号/邮箱");
        }
        if (StringUtil.isEmpty(password)) {
            isParamValid = false;
            signInObj.setErrMsgPassword("密码不允许为空");
        }
        if (!this.checkImgVerifyCode(session, verifyCode)) {
            isParamValid = false;
            signInObj.setErrMsgVerifyCode("验证码不正确");
        }
        //参数格式正确，去验证状态
        UserAccount tempData = null;
        if (isParamValid) {
            if (CommonUtil.isMobile(account)) {
                tempData = userService.getUserByMobile(account);
            } else {
                tempData = userService.getUserByEmail(account);
            }
            if (null == tempData) {
                //账户不存在
                isParamValid = false;
                signInObj.setErrMsg("登录失败，账号或密码有误");
            }
        }

        //状态正确，去验证密码
        if (isParamValid) {
            UserAccount userAccount = userService.checkPassword(account, password);
            if (null != userAccount) {
                //如果需要自动登录
                if (null != isAutoSign && isAutoSign == true) {
                    //设置自动登录cookies
                    String userAuthString = request.getHeader("user-agent");
                    if (null != userAuthString) {
                        userAuthString.replaceAll("\\(|\\)", "");
                    }
                    List<Cookie> autoSigninCookies = CookieUtil.generateAutoSigninCookies(userAccount.getUserAccount(), userAuthString, AUTO_SIGN_VALID_DATE, request.getContextPath());
                    if (null != autoSigninCookies && autoSigninCookies.size() > 0) {
                        for (Cookie cookie : autoSigninCookies) {
                            response.addCookie(cookie);     //新增cookie
                        }
                    }
                }

                //登录成功，检查账户状态，若是无效账户，跳转到“激活”页面
                session.setAttribute("userAccount", userAccount);
                return "redirect:/index.html";
            } else {
                signInObj.setErrMsg("登录失败，账号或密码有误");
                //检查是否需要把账户锁定（限制登录）

            }
        } else {
            log.warn("登录失败");
        }
        return "user/signin";
    }


    /**
     * 退出登录
     */
    @RequestMapping(value = "/user/signout.html", method = RequestMethod.GET)
    public String signout(HttpServletRequest request, HttpServletResponse response, Model model) {
        HttpSession session = request.getSession();
        session.setAttribute("userAccount", null);
        session.invalidate();       //销毁session
        Cookie[] cookies = request.getCookies();
        if (null != cookies && cookies.length > 0) {
            for (Cookie cur : cookies) {
                cur.setValue(null);
                cur.setMaxAge(0);
                cur.setPath("/" + request.getContextPath());
                response.addCookie(cur);        //删除cookie
            }
        }
        return "user/signin";
    }

    private boolean checkImgVerifyCode(HttpSession session, String imgVerifyCode) {
        if (null == imgVerifyCode) {
            return false;
        }
        String verifyCodeSaved = null;
        try {
            verifyCodeSaved = (String) session.getAttribute("imgVerifyCode");
            session.setAttribute("imgVerifyCode", null);                        //比较之后，置空
        } catch (Exception e) {
        }
        if (null == verifyCodeSaved) {
            return false;
        }
        return verifyCodeSaved.equalsIgnoreCase(imgVerifyCode);
    }


}

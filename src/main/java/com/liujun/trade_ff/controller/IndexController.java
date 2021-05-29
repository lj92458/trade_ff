package com.liujun.trade_ff.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

@Controller
public class IndexController {
    private static Logger log = LoggerFactory.getLogger(IndexController.class);

    /**
     * 首页
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(HttpServletRequest request, Model model) {
        return this.index2(request, model);
    }

    /**
     * 首页
     */
    @RequestMapping(value = "/index.html", method = RequestMethod.GET)
    public String index2(HttpServletRequest request, Model model) {
        return "index";
    }


    /**
     * 站点地图
     */
    @RequestMapping(value = "/siteMap.html", method = RequestMethod.GET)
    public String siteMap(HttpServletRequest request, Model model) {
        return "site_map";
    }

    @RequestMapping(value = "/menu_over_coin.html", method = RequestMethod.GET)
    public String overCoin(HttpServletRequest request, Model model) {
        return "over_coin";
    }

    @RequestMapping(value = "/menu_over_futuer.html", method = RequestMethod.GET)
    public String overFutuer(HttpServletRequest request, Model model) {
        return "over_futuer";
    }


    @RequestMapping(value = "/menu_quant.html", method = RequestMethod.GET)
    public String quant(HttpServletRequest request, Model model) {
        return "quant";
    }

    @RequestMapping(value = "/menu_low_rate.html", method = RequestMethod.GET)
    public String lowRate(HttpServletRequest request, Model model) {
        return "low_rate";
    }

    @RequestMapping(value = "/menu_cfmm.html", method = RequestMethod.GET)
    public String cfmm(HttpServletRequest request, Model model) {
        return "cfmm";
    }
    @RequestMapping(value = "/menu_dex.html", method = RequestMethod.GET)
    public String dex(HttpServletRequest request, Model model) {
        return "dex";
    }
}

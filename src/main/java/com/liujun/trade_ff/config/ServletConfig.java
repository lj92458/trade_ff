package com.liujun.trade_ff.config;

import com.liujun.trade_ff.common.filter.SessionFilter;
import com.liujun.trade_ff.common.filter.XSSFilter;
import org.apache.catalina.startup.Tomcat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

import javax.servlet.Servlet;

/**
 * 不好分类的，各种闲杂的bean
 */
@Configuration
public class ServletConfig {

    /*
    定义错误页面
     */
    /*
    @Bean
    @ConditionalOnClass({ Servlet.class, Server.class, Loader.class, WebAppContext.class })
    public NettyWebServerFactoryCustomizer nettyServerWiretapCustomizer(
            Environment environment, ServerProperties serverProperties) {
        return  (NettyReactiveWebServerFactory factory) ->{
                factory.addServerCustomizers(httpServer -> httpServer.wiretap(true));
                super.customize(factory);
            };

    } */

    /*
    定义错误页面
     */
    @Bean
    @ConditionalOnClass({Servlet.class, Tomcat.class})
    public TomcatWebServerFactoryCustomizer TomcatServerWiretapCustomizer(
            Environment environment, ServerProperties serverProperties) {
        return new TomcatWebServerFactoryCustomizer(environment, serverProperties) {
            @Override
            public void customize(ConfigurableTomcatWebServerFactory factory) {
                factory.addErrorPages(
                        new ErrorPage(HttpStatus.BAD_REQUEST, "/views/error/error_40x.html"),
                        new ErrorPage(HttpStatus.FORBIDDEN, "/views/error/error_40x.html"),
                        new ErrorPage(HttpStatus.NOT_FOUND, "/views/error/error_40x.html"),
                        new ErrorPage(HttpStatus.METHOD_NOT_ALLOWED, "/views/error/error_40x.html"),
                        new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/views/error/error_50x.html"),
                        new ErrorPage(Exception.class, "/views/error/error_50x.html")
                );

                super.customize(factory);
            }

        };
    }

    /*
        session过滤器
     */
    @Bean
    public FilterRegistrationBean<SessionFilter> filterRegistrationBean_session() {
        //新建过滤器注册类
        FilterRegistrationBean<SessionFilter> registration = new FilterRegistrationBean<>();
        // 添加我们写好的过滤器
        registration.setFilter(new SessionFilter());
        // 设置过滤器的URL模式
        registration.addUrlPatterns("/*");
        return registration;
    }

    /*
        xss过滤器
     */
    @Bean
    public FilterRegistrationBean<XSSFilter> filterRegistrationBean_xss() {
        //新建过滤器注册类
        FilterRegistrationBean<XSSFilter> registration = new FilterRegistrationBean<>();
        // 添加我们写好的过滤器
        registration.setFilter(new XSSFilter());
        // 设置过滤器的URL模式
        registration.addUrlPatterns("/*");
        return registration;
    }


}

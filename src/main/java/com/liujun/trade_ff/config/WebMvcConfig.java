package com.liujun.trade_ff.config;

import com.liujun.trade_ff.common.MessageLocaleResolver;
import com.liujun.trade_ff.common.exception.ExceptionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        // 参数名
        lci.setParamName("language");
        return lci;
    }

    @Bean
    public MessageLocaleResolver messageLocaleResolver() {
        return new MessageLocaleResolver();
    }

    @Bean
    public ExceptionResolver exceptionResolver() {
        return new ExceptionResolver();
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());

    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        registry.addViewController("/").setViewName("forward:/index.html");


    }
}

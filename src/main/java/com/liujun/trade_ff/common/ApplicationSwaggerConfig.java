package com.liujun.trade_ff.common;

import org.springframework.context.annotation.Bean;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger for java doc
 * Created by WuShaotong on 2016/7/19.
 */
@EnableSwagger2
public class ApplicationSwaggerConfig {

    @Bean
    public Docket resourcesDocket(){
        Docket docket = new Docket(DocumentationType.SWAGGER_2);
        ApiInfo apiInfo = new ApiInfo("Restful API", "API Document管理（请仅关注resource-api）", "1.0.0","#","liujun","","");
        docket.apiInfo(apiInfo);
        return  docket;
    }
}

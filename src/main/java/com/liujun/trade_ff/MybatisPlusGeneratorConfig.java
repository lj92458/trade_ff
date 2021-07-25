package com.liujun.trade_ff;

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;

import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.apache.commons.lang3.StringUtils;

import java.util.Scanner;

//https://blog.csdn.net/qq_14926283/article/details/110038480
//https://www.cnblogs.com/liuyj-top/p/12976396.html
//https://gitee.com/baomidou/mybatis-plus-samples/blob/master/mybatis-plus-sample-generator/src/main/java/com/baomidou/mybatisplus/samples/generator/MysqlGenerator.java
public class MybatisPlusGeneratorConfig {

    public static String scanner(String tip) {
        Scanner scanner = new Scanner(System.in);
        StringBuilder help = new StringBuilder();
        help.append("请输入" + tip + "：");
        System.out.println(help.toString());
        if (scanner.hasNext()) {
            String ipt = scanner.next();
            if (StringUtils.isNotEmpty(ipt)) {
                return ipt;
            }
        }
        throw new MybatisPlusException("请输入正确的" + tip + "！");
    }

    public static void main(String[] args) {

        // 代码生成器
        AutoGenerator mpg = new AutoGenerator(new DataSourceConfig.Builder(
                "jdbc:mysql://127.0.0.1:3306/demo?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true",
                "usrname",
                "pwd").build())
                .global(new GlobalConfig.Builder()
                        .outputDir(System.getProperty("user.dir") + "/src/main/java")
                        .author("astupidcoder")
                        .openDir(false) //是否打开指定的目录
                        //.enableSwagger() //实体属性加上 Swagger2 注解
                        .build()
                ).packageInfo(new PackageConfig.Builder()
                        .moduleName(scanner("模块名"))
                        .parent("com.example")
                        .entity("model.auto")
                        .mapper("mapper.auto")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .build()
                );


        // 配置模板
        TemplateConfig templateConfig = new TemplateConfig.Builder().build();

        mpg.template(templateConfig);

        // 策略配置
        StrategyConfig strategy = new StrategyConfig.Builder()
                //.addTablePrefix("t" + "_")
                .build();


        mpg.strategy(strategy);
        mpg.execute(new FreemarkerTemplateEngine());

    }
}

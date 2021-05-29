package com.liujun.trade_ff;

import com.liujun.trade_ff.common.CustomizedPropertyPlaceholderConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;

@SpringBootApplication
public class TradeFfApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeFfApplication.class, args);
	}



}

package com.dji.sample;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan({"com.dji.sample.**.dao"})//, "com.cleaner.**.dao"
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.dji",
		"com.df","com.cleaner.djuav.controller",//
		"com.cleaner.djuav.service",
		"com.cleaner.djuav.util",
		"com.cleaner.djuav.domain",
		//"com.cleaner.djuav.config"
})//, "com.cleaner"
//@Import(com.cleaner.djuav.config.WaylineJacksonConfig.class) // dfe-wayline单独只导入这个 Bean
public class CloudApiSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudApiSampleApplication.class, args);
	}

}

package com.kh.midpoint;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kh.midpoint.**.model.dao")
public class MidPointApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MidPointApiApplication.class, args);
	}

}

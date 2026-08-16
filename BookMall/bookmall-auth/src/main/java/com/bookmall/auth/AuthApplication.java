package com.bookmall.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.bookmall.auth.mapper")
@SpringBootApplication(scanBasePackages = {"com.bookmall.auth", "com.bookmall.common"})
public class AuthApplication {

    public static void main(String[] args) {

        SpringApplication.run(AuthApplication.class, args);
    }
}
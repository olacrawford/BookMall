package com.bookmall.aftersale;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.bookmall.aftersale.mapper")
@EnableFeignClients(basePackages = "com.bookmall.aftersale.client")
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.bookmall.aftersale", "com.bookmall.common"})
public class AfterSaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterSaleApplication.class, args);
    }
}

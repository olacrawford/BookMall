package com.bookmall.aftersale;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.bookmall.aftersale.mapper")
@SpringBootApplication(scanBasePackages = {"com.bookmall.aftersale", "com.bookmall.common"})
public class AfterSaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterSaleApplication.class, args);
    }
}

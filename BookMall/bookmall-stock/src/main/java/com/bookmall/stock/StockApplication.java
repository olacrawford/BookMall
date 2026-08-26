package com.bookmall.stock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.bookmall.stock.mapper")
@SpringBootApplication(scanBasePackages = {"com.bookmall.stock", "com.bookmall.common"})
public class StockApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class, args);
    }
}

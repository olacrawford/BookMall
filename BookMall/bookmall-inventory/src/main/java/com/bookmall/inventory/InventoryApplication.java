package com.bookmall.inventory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.bookmall.inventory.mapper")
@SpringBootApplication(scanBasePackages = {"com.bookmall.inventory", "com.bookmall.common"})
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
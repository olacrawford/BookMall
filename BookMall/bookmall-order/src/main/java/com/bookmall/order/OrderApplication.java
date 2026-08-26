package com.bookmall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.bookmall.order.mapper")
@EnableFeignClients(basePackages = "com.bookmall.order.client")
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.bookmall.order", "com.bookmall.common"})
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}

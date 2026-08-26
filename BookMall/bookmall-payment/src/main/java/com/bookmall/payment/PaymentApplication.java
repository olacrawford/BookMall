package com.bookmall.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.bookmall.payment.mapper")
@EnableFeignClients(basePackages = "com.bookmall.payment.client")
@SpringBootApplication(scanBasePackages = {"com.bookmall.payment", "com.bookmall.common"})
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}

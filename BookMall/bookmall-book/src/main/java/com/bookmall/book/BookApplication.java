package com.bookmall.book;

import com.bookmall.book.config.CacheProperties;
import com.bookmall.book.config.SentinelRuleProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan("com.bookmall.book.mapper")
@EnableConfigurationProperties({CacheProperties.class, SentinelRuleProperties.class})
@SpringBootApplication(scanBasePackages = {"com.bookmall.book", "com.bookmall.common"})
public class BookApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookApplication.class, args);
    }
}

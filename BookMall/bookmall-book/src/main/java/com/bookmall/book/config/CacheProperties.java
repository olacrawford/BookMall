package com.bookmall.book.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bookmall.cache")
public class CacheProperties {

    private long bookListTtlMinutes = 10;
    private long bookDetailTtlMinutes = 30;
    private long categoryTreeTtlMinutes = 30;
    private long emptyTtlMinutes = 5;

}

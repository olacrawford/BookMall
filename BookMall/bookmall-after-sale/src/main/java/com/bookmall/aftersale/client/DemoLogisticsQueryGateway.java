package com.bookmall.aftersale.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoLogisticsQueryGateway implements LogisticsQueryGateway {

    private static final Logger log = LoggerFactory.getLogger(DemoLogisticsQueryGateway.class);

    private final String mode;

    public DemoLogisticsQueryGateway(@Value("${after-sale.logistics.mode:ok}") String mode) {
        this.mode = mode == null ? "ok" : mode;
    }

    @Override
    public LogisticsQueryResult query(Long afterSaleId, Long orderId) {
        if ("timeout".equalsIgnoreCase(mode)) {
            log.warn("logistics timeout injected afterSaleId={} orderId={}", afterSaleId, orderId);
            return LogisticsQueryResult.timedOut();
        }
        if ("unavailable".equalsIgnoreCase(mode)) {
            log.warn("logistics unavailable injected afterSaleId={} orderId={}", afterSaleId, orderId);
            return LogisticsQueryResult.unavailable();
        }
        return LogisticsQueryResult.ok();
    }
}

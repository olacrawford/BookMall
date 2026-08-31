package com.bookmall.aftersale.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundVO {

    private Long id;
    private String refundNo;
    private Long afterSaleId;
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private String idempotencyKey;
    private LocalDateTime createTime;
}

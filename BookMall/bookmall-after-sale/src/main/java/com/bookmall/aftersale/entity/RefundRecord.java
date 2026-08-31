package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_refund_record")
public class RefundRecord {

    private Long id;
    private String refundNo;
    private Long afterSaleId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String idempotencyKey;
    private String providerRef;
    private String errorCode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

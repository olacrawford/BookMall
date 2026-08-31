package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_after_sale_order")
public class AfterSaleOrder {

    private Long id;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String type;
    private String status;
    private BigDecimal amount;
    private String policyVersion;
    private String idempotencyKey;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.bookmall.aftersale.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AfterSaleDetailVO {

    private Long id;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private String type;
    private String status;
    private BigDecimal amount;
    private String policyVersion;
    private Long ticketId;
    private String ticketNo;
    private String description;
    private String decisionStatus;
    private Long workflowId;
    private String workflowStatus;
    private LocalDateTime createTime;
}

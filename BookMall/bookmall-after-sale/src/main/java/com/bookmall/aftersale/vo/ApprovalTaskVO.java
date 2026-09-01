package com.bookmall.aftersale.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalTaskVO {

    private Long id;
    private String taskNo;
    private Long afterSaleId;
    private Long orderId;
    private Long userId;
    private String status;
    private String decision;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime decidedAt;
}

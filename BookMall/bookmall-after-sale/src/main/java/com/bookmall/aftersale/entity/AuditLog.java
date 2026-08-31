package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_audit_log")
public class AuditLog {

    private Long id;
    private String traceId;
    private Long ticketId;
    private Long afterSaleId;
    private String operatorType;
    private Long operatorId;
    private String action;
    private String beforeStatus;
    private String afterStatus;
    private String detailJson;
    private LocalDateTime createTime;
}

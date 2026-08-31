package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_after_sale_ticket")
public class AfterSaleTicket {

    private Long id;
    private String ticketNo;
    private Long afterSaleId;
    private Long userId;
    private String description;
    private String decisionStatus;
    private String traceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

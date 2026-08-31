package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_approval_task")
public class ApprovalTask {

    private Long id;
    private String taskNo;
    private Long workflowId;
    private Long ticketId;
    private Long assigneeId;
    private String status;
    private String decision;
    private String comment;
    private LocalDateTime decidedAt;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

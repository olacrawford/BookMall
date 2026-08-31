package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_workflow_step")
public class WorkflowStep {

    private Long id;
    private Long workflowId;
    private String stepKey;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime leaseUntil;
    private String checkpointJson;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

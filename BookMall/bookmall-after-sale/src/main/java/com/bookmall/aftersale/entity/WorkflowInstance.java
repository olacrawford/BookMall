package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_workflow_instance")
public class WorkflowInstance {

    private Long id;
    private String workflowNo;
    private Long ticketId;
    private String workflowType;
    private String status;
    private String currentStep;
    private String workflowVersion;
    private String contextJson;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

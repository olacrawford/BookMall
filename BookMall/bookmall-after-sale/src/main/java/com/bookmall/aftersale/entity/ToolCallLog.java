package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_tool_call_log")
public class ToolCallLog {

    private Long id;
    private Long ticketId;
    private String traceId;
    private String toolName;
    private String argumentsJson;
    private Integer success;
    private Integer latencyMs;
    private String errorCode;
    private LocalDateTime createTime;
}

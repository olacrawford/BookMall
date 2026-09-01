package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_decision")
public class AiDecisionRecord {

    private Long id;
    private Long ticketId;
    private String traceId;
    private String provider;
    private String modelName;
    private String promptVersion;
    private String decisionJson;
    private String validationStatus;
    private String rawOutput;
    private Integer latencyMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private LocalDateTime createTime;
}

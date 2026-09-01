package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_evidence")
public class AiEvidence {

    private Long id;
    private Long decisionId;
    private String evidenceId;
    private String evidenceType;
    private String sourceRef;
    private String content;
    private String policyVersion;
    private String permissionScope;
    private LocalDateTime createTime;
}

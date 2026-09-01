package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.entity.AiDecisionRecord;
import com.bookmall.aftersale.entity.AiEvidence;
import com.bookmall.aftersale.entity.AuditLog;
import com.bookmall.aftersale.entity.ToolCallLog;
import com.bookmall.aftersale.mapper.AiDecisionRecordMapper;
import com.bookmall.aftersale.mapper.AiEvidenceMapper;
import com.bookmall.aftersale.mapper.AuditLogMapper;
import com.bookmall.aftersale.mapper.ToolCallLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiAuditService {

    private final AiDecisionRecordMapper decisionRecordMapper;
    private final AiEvidenceMapper evidenceMapper;
    private final ToolCallLogMapper toolCallLogMapper;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAuditService(AiDecisionRecordMapper decisionRecordMapper,
                          AiEvidenceMapper evidenceMapper,
                          ToolCallLogMapper toolCallLogMapper,
                          AuditLogMapper auditLogMapper) {
        this.decisionRecordMapper = decisionRecordMapper;
        this.evidenceMapper = evidenceMapper;
        this.toolCallLogMapper = toolCallLogMapper;
        this.auditLogMapper = auditLogMapper;
    }

    public Long saveDecision(AiDecisionRecord record) {
        decisionRecordMapper.insert(record);
        return record.getId();
    }

    public void saveEvidence(AiEvidence evidence) {
        evidenceMapper.insert(evidence);
    }

    public void saveToolCall(ToolCallLog log) {
        toolCallLogMapper.insert(log);
    }

    public void saveAudit(AuditLog audit) {
        auditLogMapper.insert(audit);
    }

    public String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}

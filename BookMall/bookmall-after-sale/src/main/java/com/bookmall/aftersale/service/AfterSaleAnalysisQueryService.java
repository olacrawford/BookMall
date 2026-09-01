package com.bookmall.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.aftersale.ai.model.AiAnalysisResponse;
import com.bookmall.aftersale.ai.model.AiDecision;
import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.entity.AfterSaleOrder;
import com.bookmall.aftersale.entity.AfterSaleTicket;
import com.bookmall.aftersale.entity.AiDecisionRecord;
import com.bookmall.aftersale.entity.AiEvidence;
import com.bookmall.aftersale.entity.ToolCallLog;
import com.bookmall.aftersale.mapper.AfterSaleOrderMapper;
import com.bookmall.aftersale.mapper.AfterSaleTicketMapper;
import com.bookmall.aftersale.mapper.AiDecisionRecordMapper;
import com.bookmall.aftersale.mapper.AiEvidenceMapper;
import com.bookmall.aftersale.mapper.ToolCallLogMapper;
import com.bookmall.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AfterSaleAnalysisQueryService {

    private final AfterSaleOrderMapper afterSaleOrderMapper;
    private final AfterSaleTicketMapper afterSaleTicketMapper;
    private final AiDecisionRecordMapper aiDecisionRecordMapper;
    private final AiEvidenceMapper aiEvidenceMapper;
    private final ToolCallLogMapper toolCallLogMapper;
    private final AfterSaleAccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public AfterSaleAnalysisQueryService(AfterSaleOrderMapper afterSaleOrderMapper,
                                         AfterSaleTicketMapper afterSaleTicketMapper,
                                         AiDecisionRecordMapper aiDecisionRecordMapper,
                                         AiEvidenceMapper aiEvidenceMapper,
                                         ToolCallLogMapper toolCallLogMapper,
                                         AfterSaleAccessGuard accessGuard,
                                         ObjectMapper objectMapper) {
        this.afterSaleOrderMapper = afterSaleOrderMapper;
        this.afterSaleTicketMapper = afterSaleTicketMapper;
        this.aiDecisionRecordMapper = aiDecisionRecordMapper;
        this.aiEvidenceMapper = aiEvidenceMapper;
        this.toolCallLogMapper = toolCallLogMapper;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    public AiAnalysisResponse getAnalysis(Long userId, Long afterSaleId) {
        AfterSaleOrder order = afterSaleOrderMapper.selectById(afterSaleId);
        if (order == null) {
            throw new BusinessException(404, "售后单不存在");
        }
        accessGuard.requireOwner(order, userId);

        AfterSaleTicket ticket = afterSaleTicketMapper.selectOne(new LambdaQueryWrapper<AfterSaleTicket>()
                .eq(AfterSaleTicket::getAfterSaleId, afterSaleId));
        if (ticket == null) {
            throw new BusinessException(404, "售后工单不存在");
        }

        AiDecisionRecord record = latestDecision(ticket.getId());
        if (record == null) {
            throw new BusinessException(404, "暂无 AI 分析，工单已转人工");
        }
        AiDecision decision = parseDecision(record.getDecisionJson());

        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setTraceId(record.getTraceId());
        response.setTicketId(record.getTicketId());
        response.setIntent(decision.getIntent());
        response.setDecision(decision);
        response.setValidationStatus(record.getValidationStatus());
        response.setToolResults(toToolResults(record.getTraceId()));
        response.setRuleHits(toRuleHits(record.getId()));
        enrichToolData(response, record.getId());
        return response;
    }

    private AiDecisionRecord latestDecision(Long ticketId) {
        return aiDecisionRecordMapper.selectOne(new LambdaQueryWrapper<AiDecisionRecord>()
                .eq(AiDecisionRecord::getTicketId, ticketId)
                .orderByDesc(AiDecisionRecord::getId)
                .last("LIMIT 1"));
    }

    private AiDecision parseDecision(String decisionJson) {
        try {
            return objectMapper.readValue(decisionJson, AiDecision.class);
        } catch (Exception ex) {
            throw new BusinessException(500, "AI 决策记录无法解析");
        }
    }

    private List<ToolResult> toToolResults(String traceId) {
        List<ToolCallLog> logs = toolCallLogMapper.selectList(new LambdaQueryWrapper<ToolCallLog>()
                .eq(ToolCallLog::getTraceId, traceId)
                .orderByAsc(ToolCallLog::getId));
        List<ToolResult> results = new ArrayList<>();
        for (ToolCallLog log : logs) {
            results.add(new ToolResult(
                    log.getToolName(),
                    log.getSuccess() != null && log.getSuccess() == 1,
                    log.getTraceId(),
                    log.getLatencyMs() == null ? 0L : log.getLatencyMs().longValue(),
                    log.getErrorCode(),
                    new LinkedHashMap<>(),
                    readArguments(log.getArgumentsJson())));
        }
        return results;
    }

    private List<RuleHit> toRuleHits(Long decisionId) {
        List<RuleHit> hits = new ArrayList<>();
        List<AiEvidence> evidenceList = aiEvidenceMapper.selectList(new LambdaQueryWrapper<AiEvidence>()
                .eq(AiEvidence::getDecisionId, decisionId)
                .eq(AiEvidence::getEvidenceType, "POLICY")
                .orderByAsc(AiEvidence::getId));
        for (AiEvidence evidence : evidenceList) {
            hits.add(toRuleHit(evidence));
        }
        return hits;
    }

    private void enrichToolData(AiAnalysisResponse response, Long decisionId) {
        List<AiEvidence> evidenceList = aiEvidenceMapper.selectList(new LambdaQueryWrapper<AiEvidence>()
                .eq(AiEvidence::getDecisionId, decisionId)
                .ne(AiEvidence::getEvidenceType, "POLICY"));
        for (AiEvidence evidence : evidenceList) {
            response.getToolResults().stream()
                    .filter(result -> evidence.getSourceRef().equals(result.getToolName()))
                    .findFirst()
                    .ifPresent(result -> result.setData(readContent(evidence.getContent())));
        }
    }

    private RuleHit toRuleHit(AiEvidence evidence) {
        RuleHit hit = new RuleHit();
        hit.setContent(evidence.getContent());
        hit.setPolicyVersion(evidence.getPolicyVersion());
        hit.setPermissionScope(evidence.getPermissionScope());
        String[] parts = evidence.getEvidenceId() == null ? new String[0] : evidence.getEvidenceId().split("#");
        if (parts.length >= 2) {
            hit.setDocumentCode(parts[1]);
        }
        if (parts.length >= 3) {
            try {
                hit.setChunkNo(Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) {
                hit.setChunkNo(null);
            }
        }
        return hit;
    }

    private Map<String, Object> readArguments(String json) {
        return readMap(json);
    }

    private Map<String, Object> readContent(String json) {
        return readMap(json);
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }
}

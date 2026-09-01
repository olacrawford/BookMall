package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.AiAnalysisResponse;
import com.bookmall.aftersale.ai.model.AiDecision;
import com.bookmall.aftersale.ai.model.DecisionValidation;
import com.bookmall.aftersale.ai.model.EvidenceCollection;
import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.entity.AiDecisionRecord;
import com.bookmall.aftersale.entity.AiEvidence;
import com.bookmall.aftersale.entity.AuditLog;
import com.bookmall.aftersale.entity.ToolCallLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AfterSaleAiAnalysisService {

    private final IntentAgent intentAgent;
    private final EvidenceAgent evidenceAgent;
    private final LlmClient llmClient;
    private final JsonDecisionParser decisionParser;
    private final ComplianceAgent complianceAgent;
    private final AiAuditService aiAuditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AfterSaleAiAnalysisService(IntentAgent intentAgent,
                                      EvidenceAgent evidenceAgent,
                                      LlmClient llmClient,
                                      JsonDecisionParser decisionParser,
                                      ComplianceAgent complianceAgent,
                                      AiAuditService aiAuditService) {
        this.intentAgent = intentAgent;
        this.evidenceAgent = evidenceAgent;
        this.llmClient = llmClient;
        this.decisionParser = decisionParser;
        this.complianceAgent = complianceAgent;
        this.aiAuditService = aiAuditService;
    }

    public AiAnalysisResponse analyze(TicketContext context) {
        long started = System.currentTimeMillis();
        prepareContext(context);
        String traceId = context.getTraceId();
        String intent = intentAgent.extract(context);
        EvidenceCollection evidence = evidenceAgent.collect(context);

        String rawOutput = llmClient.generateDecision(context, intent, evidence.getToolResults(), evidence.getRuleHits());
        AiDecision decision;
        String validationStatus;
        List<String> validationErrors = new ArrayList<>();
        try {
            decision = decisionParser.parse(rawOutput);
            DecisionValidation validation = complianceAgent.assess(decision);
            if (validation.isValid()) {
                validationStatus = "VALID";
            } else {
                validationStatus = "INVALID";
                validationErrors.addAll(validation.getErrors());
            }
        } catch (JsonProcessingException | RuntimeException ex) {
            decision = null;
            validationStatus = "INVALID_JSON";
            validationErrors.add("LLM 输出无法解析为 Decision JSON");
        }

        if (!"VALID".equals(validationStatus)) {
            decision = AiDecision.fallbackHuman(context.getTicketId(), context.getPolicyVersion(),
                    "AI 输出未通过 Schema 校验，转人工");
            validationStatus = "FALLBACK_HUMAN";
        }

        AiDecisionRecord record = new AiDecisionRecord();
        record.setTicketId(context.getTicketId());
        record.setTraceId(traceId);
        record.setProvider(llmClient.provider());
        record.setModelName(llmClient.modelName());
        record.setPromptVersion("phase4-v1");
        record.setDecisionJson(toJson(decision));
        record.setValidationStatus(validationStatus);
        record.setRawOutput(rawOutput);
        record.setLatencyMs((int) (System.currentTimeMillis() - started));
        record.setInputTokens(0);
        record.setOutputTokens(0);
        record.setCreateTime(LocalDateTime.now());
        Long decisionId = aiAuditService.saveDecision(record);

        saveEvidence(decisionId, context, evidence);
        saveToolCalls(evidence.getToolResults());
        saveAudit(record, decision);

        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setTraceId(traceId);
        response.setTicketId(context.getTicketId());
        response.setIntent(intent);
        response.setDecision(decision);
        response.setValidationStatus(validationStatus);
        response.setToolResults(evidence.getToolResults());
        response.setRuleHits(evidence.getRuleHits());
        response.setValidationErrors(validationErrors);
        return response;
    }

    private void prepareContext(TicketContext context) {
        if (context == null) {
            throw new IllegalArgumentException("ticketContext must not be null");
        }
        if (context.getTraceId() == null || context.getTraceId().isBlank()) {
            context.setTraceId("tr-" + UUID.randomUUID());
        }
        if (context.getPolicyVersion() == null || context.getPolicyVersion().isBlank()) {
            context.setPolicyVersion("v1");
        }
        if (context.getUserEvidence() == null) {
            context.setUserEvidence(new ArrayList<>());
        }
    }

    private void saveEvidence(Long decisionId, TicketContext context, EvidenceCollection evidence) {
        for (ToolResult result : evidence.getToolResults()) {
            if (!result.isSuccess()) {
                continue;
            }
            String evidenceId = String.valueOf(result.getData().getOrDefault("evidenceId", ""));
            if (evidenceId.isBlank()) {
                continue;
            }
            AiEvidence item = new AiEvidence();
            item.setDecisionId(decisionId);
            item.setEvidenceId(evidenceId);
            item.setEvidenceType(evidenceType(evidenceId));
            item.setSourceRef(result.getToolName());
            item.setContent(String.valueOf(result.getData()));
            item.setPolicyVersion(context.getPolicyVersion());
            item.setPermissionScope("PUBLIC");
            item.setCreateTime(LocalDateTime.now());
            aiAuditService.saveEvidence(item);
        }
        for (RuleHit hit : evidence.getRuleHits()) {
            String evidenceId = hit.evidenceId();
            AiEvidence item = new AiEvidence();
            item.setDecisionId(decisionId);
            item.setEvidenceId(evidenceId);
            item.setEvidenceType("POLICY");
            item.setSourceRef("query_after_sale_rule");
            item.setContent(hit.getContent());
            item.setPolicyVersion(hit.getPolicyVersion());
            item.setPermissionScope("PUBLIC");
            item.setCreateTime(LocalDateTime.now());
            aiAuditService.saveEvidence(item);
        }
    }

    private void saveToolCalls(List<ToolResult> results) {
        for (ToolResult result : results) {
            ToolCallLog log = new ToolCallLog();
            log.setTicketId(ticketIdOf(result));
            log.setTraceId(result.getTraceId());
            log.setToolName(result.getToolName());
            log.setArgumentsJson(aiAuditService.toJson(result.getArguments()));
            log.setSuccess(result.isSuccess() ? 1 : 0);
            log.setLatencyMs(result.getLatencyMs() == null ? 0 : result.getLatencyMs().intValue());
            log.setErrorCode(result.getErrorCode());
            log.setCreateTime(LocalDateTime.now());
            aiAuditService.saveToolCall(log);
        }
    }

    private Long ticketIdOf(ToolResult result) {
        Object ticketId = result.getArguments() == null ? null : result.getArguments().get("ticketId");
        if (ticketId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private void saveAudit(AiDecisionRecord record, AiDecision decision) {
        AuditLog audit = new AuditLog();
        audit.setTraceId(record.getTraceId());
        audit.setTicketId(record.getTicketId());
        audit.setOperatorType("AI");
        audit.setAction("AI_ANALYZE");
        audit.setAfterStatus(decision.getNextStep());
        audit.setDetailJson(toJson(decision));
        audit.setCreateTime(LocalDateTime.now());
        aiAuditService.saveAudit(audit);
    }

    private String evidenceType(String evidenceId) {
        String lower = evidenceId.toLowerCase();
        if (lower.startsWith("order:")) {
            return "ORDER";
        }
        if (lower.startsWith("logistics:")) {
            return "LOGISTICS";
        }
        if (lower.startsWith("risk:")) {
            return "RISK";
        }
        if (lower.startsWith("policy:")) {
            return "POLICY";
        }
        return "USER";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}

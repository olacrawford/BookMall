package com.bookmall.aftersale.ai.service.impl;

import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.ai.service.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MockLlmClient implements LlmClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateDecision(TicketContext context, String intent,
                                   List<ToolResult> toolResults, List<RuleHit> ruleHits) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("intent", intent == null ? "UNKNOWN" : intent);
        decision.put("action", "NEEDS_HUMAN");
        decision.put("amount", amount(context));
        decision.put("reason", "Mock LLM 建议先人工核实签收凭证后处置，不自动退款。");
        decision.put("riskLevel", riskLevel(toolResults));
        decision.put("evidenceIds", evidenceIds(toolResults, ruleHits));
        decision.put("policyVersion", context.getPolicyVersion() == null ? "v1" : context.getPolicyVersion());
        decision.put("nextStep", "UNDER_REVIEW");
        try {
            return objectMapper.writeValueAsString(decision);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Mock LLM output serialization failed", ex);
        }
    }

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public String modelName() {
        return "mock-llm-phase4";
    }

    private BigDecimal amount(TicketContext context) {
        if (context == null || context.getOrderSnapshot() == null || context.getOrderSnapshot().getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        return context.getOrderSnapshot().getTotalAmount();
    }

    private String riskLevel(List<ToolResult> toolResults) {
        return toolResults.stream()
                .filter(result -> result.isSuccess() && "query_user_risk".equals(result.getToolName()))
                .map(result -> String.valueOf(result.getData().get("riskLevel")))
                .findFirst()
                .orElse("MEDIUM");
    }

    private List<String> evidenceIds(List<ToolResult> toolResults, List<RuleHit> ruleHits) {
        List<String> ids = new ArrayList<>();
        toolResults.stream()
                .filter(ToolResult::isSuccess)
                .map(result -> result.getData().get("evidenceId"))
                .filter(id -> id != null && !String.valueOf(id).isBlank())
                .map(String::valueOf)
                .forEach(ids::add);
        ruleHits.stream()
                .filter(hit -> hit != null && hit.getPolicyVersion() != null)
                .map(RuleHit::evidenceId)
                .filter(id -> !ids.contains(id))
                .forEach(ids::add);
        return ids;
    }
}

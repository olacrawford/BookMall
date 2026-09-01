package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.EvidenceCollection;
import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.ai.tool.DomainToolRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EvidenceAgent {

    private final DomainToolRegistry toolRegistry;

    public EvidenceAgent(DomainToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public EvidenceCollection collect(TicketContext context) {
        EvidenceCollection collection = new EvidenceCollection();
        collection.getToolResults().add(invoke(context, "query_order", Map.of(
                "ticketId", context.getTicketId(),
                "orderId", context.getOrderId(),
                "userId", context.getUserId())));
        collection.getToolResults().add(invoke(context, "query_logistics", Map.of(
                "ticketId", context.getTicketId(),
                "orderId", context.getOrderId(),
                "afterSaleId", context.getTicketId() == null ? 0L : context.getTicketId())));
        collection.getToolResults().add(invoke(context, "query_delivery_proof", Map.of(
                "ticketId", context.getTicketId(),
                "userEvidence", context.getUserEvidence() == null ? List.of() : context.getUserEvidence())));
        String riskHint = context.getOrderSnapshot() == null || context.getOrderSnapshot().getTotalAmount() == null
                ? "LOW"
                : "LOW";
        collection.getToolResults().add(invoke(context, "query_user_risk", Map.of(
                "ticketId", context.getTicketId(),
                "userId", context.getUserId(),
                "riskHint", riskHint)));
        collection.getToolResults().add(invoke(context, "query_after_sale_rule", Map.of(
                "ticketId", context.getTicketId(),
                "query", context.getDescription(),
                "policyVersion", context.getPolicyVersion(),
                "permissionScope", "PUBLIC",
                "limit", 3)));

        collection.setRuleHits(extractRuleHits(collection.getToolResults()));
        return collection;
    }

    private ToolResult invoke(TicketContext context, String toolName, Map<String, Object> arguments) {
        ToolInvocation invocation = new ToolInvocation();
        invocation.setToolName(toolName);
        invocation.setTraceId(context.getTraceId());
        invocation.setTicketId(context.getTicketId());
        invocation.setArguments(new LinkedHashMap<>(arguments));
        return toolRegistry.invoke(invocation);
    }

    private List<RuleHit> extractRuleHits(List<ToolResult> results) {
        return results.stream()
                .filter(result -> "query_after_sale_rule".equals(result.getToolName()) && result.isSuccess())
                .flatMap(result -> {
                    Object hits = result.getData().get("hits");
                    if (!(hits instanceof List<?> list)) {
                        return java.util.stream.Stream.empty();
                    }
                    return list.stream().map(this::toRuleHit).filter(hit -> hit != null);
                })
                .toList();
    }

    private RuleHit toRuleHit(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        RuleHit hit = new RuleHit();
        hit.setChunkId(longValue(map.get("chunkId")));
        hit.setDocumentCode(text(map.get("documentCode")));
        hit.setTitle(text(map.get("title")));
        hit.setCategory(text(map.get("category")));
        hit.setChunkNo(intValue(map.get("chunkNo")));
        hit.setContent(text(map.get("content")));
        hit.setPolicyVersion(text(map.get("policyVersion")));
        Object score = map.get("score");
        if (score instanceof Number number) {
            hit.setScore(java.math.BigDecimal.valueOf(number.doubleValue()));
        }
        if (hit.getPolicyVersion() == null && map.get("evidenceId") != null) {
            hit.setPolicyVersion(evidencePolicyVersion(text(map.get("evidenceId"))));
        }
        hit.setPermissionScope("PUBLIC");
        return hit;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String evidencePolicyVersion(String evidenceId) {
        if (evidenceId == null || !evidenceId.contains("#")) {
            return null;
        }
        String prefix = evidenceId.substring(evidenceId.indexOf(":") + 1, evidenceId.indexOf("#"));
        return prefix;
    }
}

package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryDeliveryProofTool implements DomainTool {

    @Override
    public String name() {
        return "query_delivery_proof";
    }

    @Override
    public long timeoutMs() {
        return 300;
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation) {
        List<String> userEvidence = readEvidence(invocation.getArguments().get("userEvidence"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deliveryProofAvailable", !userEvidence.isEmpty());
        data.put("proofs", userEvidence.isEmpty()
                ? List.of("MOCK_CHECK_REQUIRED")
                : userEvidence);
        data.put("evidenceId", "delivery-proof:" + invocation.getTicketId());
        if (userEvidence.isEmpty()) {
            return new ToolResult(name(), false, invocation.getTraceId(), 0L, "PROOF_MISSING", data, invocation.getArguments());
        }
        return ToolResult.ok(name(), invocation.getTraceId(), data, 0L);
    }

    private List<String> readEvidence(Object value) {
        List<String> evidence = new ArrayList<>();
        if (value instanceof List<?> values) {
            values.stream().filter(item -> item != null && !String.valueOf(item).isBlank())
                    .map(String::valueOf)
                    .forEach(evidence::add);
        } else if (value != null && !String.valueOf(value).isBlank()) {
            evidence.add(String.valueOf(value));
        }
        return evidence;
    }
}

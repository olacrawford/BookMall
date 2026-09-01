package com.bookmall.aftersale.ai.service.impl;

import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.client.dto.OrderSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockLlmClientTest {

    private final MockLlmClient client = new MockLlmClient();

    @Test
    void generateDecision_fixedNeedsHumanWithEvidence() throws Exception {
        TicketContext context = new TicketContext();
        context.setPolicyVersion("v1");
        OrderSnapshot snapshot = new OrderSnapshot();
        snapshot.setId(10001L);
        snapshot.setTotalAmount(BigDecimal.valueOf(39.80));
        context.setOrderSnapshot(snapshot);

        Map<String, Object> orderData = new LinkedHashMap<>();
        orderData.put("evidenceId", "order:10001");
        Map<String, Object> riskData = new LinkedHashMap<>();
        riskData.put("riskLevel", "LOW");
        riskData.put("evidenceId", "risk:7");
        List<ToolResult> toolResults = List.of(
                ToolResult.ok("query_order", "tr-12345678", orderData, 2L),
                ToolResult.ok("query_user_risk", "tr-12345678", riskData, 1L));

        RuleHit hit = new RuleHit();
        hit.setPolicyVersion("v1");
        hit.setChunkNo(1);
        hit.setDocumentCode("AFTER_SALE_LOGISTICS_NOT_RECEIVED");

        String json = client.generateDecision(context, "LOGISTICS_NOT_RECEIVED", toolResults, List.of(hit));

        JsonNode node = new ObjectMapper().readTree(json);
        assertEquals("NEEDS_HUMAN", node.get("action").asText());
        assertEquals("LOW", node.get("riskLevel").asText());
        assertEquals("v1", node.get("policyVersion").asText());
        assertTrue(node.get("evidenceIds").toString().contains("order:10001"));
        assertTrue(node.get("evidenceIds").toString().contains("policy:v1#AFTER_SALE_LOGISTICS_NOT_RECEIVED#1"));
        assertEquals("mock", client.provider());
    }
}

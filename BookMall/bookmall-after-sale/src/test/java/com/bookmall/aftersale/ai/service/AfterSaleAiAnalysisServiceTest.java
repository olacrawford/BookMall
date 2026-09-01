package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.EvidenceCollection;
import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.entity.AiDecisionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleAiAnalysisServiceTest {

    @Mock
    private LlmClient llmClient;

    @Mock
    private EvidenceAgent evidenceAgent;

    @Mock
    private AiAuditService aiAuditService;

    private AfterSaleAiAnalysisService service;
    private TicketContext context;

    @BeforeEach
    void setUp() {
        IntentAgent intentAgent = new IntentAgent();
        JsonDecisionParser parser = new JsonDecisionParser();
        DecisionValidator validator = new DecisionValidator();
        ComplianceAgent complianceAgent = new ComplianceAgent(validator);
        service = new AfterSaleAiAnalysisService(intentAgent, evidenceAgent, llmClient,
                parser, complianceAgent, aiAuditService);

        context = new TicketContext();
        context.setTicketId(91001L);
        context.setUserId(7L);
        context.setOrderId(10001L);
        context.setDescription("物流显示签收但我没收到包裹");
        context.setPolicyVersion("v1");
    }

    @Test
    void analyze_validMockDecision_returnsValidAndPersistsAudit() throws Exception {
        when(llmClient.generateDecision(any(), any(), any(), any())).thenReturn("""
                {"intent":"LOGISTICS_NOT_RECEIVED","action":"NEEDS_HUMAN","amount":39.80,
                 "reason":"先人工核实签收凭证","riskLevel":"LOW",
                 "evidenceIds":["order:10001","policy:v1#1"],
                 "policyVersion":"v1","nextStep":"UNDER_REVIEW"}
                """);
        when(evidenceAgent.collect(any())).thenReturn(sampleEvidence());

        var response = service.analyze(context);

        assertEquals("VALID", response.getValidationStatus());
        assertEquals("NEEDS_HUMAN", response.getDecision().getAction());
        verify(aiAuditService).saveDecision(any(AiDecisionRecord.class));
        verify(aiAuditService).saveAudit(any());
    }

    @Test
    void analyze_illegalDecision_fallsBackToHumanWithoutExecuting() throws Exception {
        when(llmClient.generateDecision(any(), any(), any(), any())).thenReturn("""
                {"intent":"LOGISTICS_NOT_RECEIVED","action":"REFUND_DIRECT","amount":39.80,
                 "reason":"直接退款","riskLevel":"LOW",
                 "evidenceIds":["order:10001"],
                 "policyVersion":"v1","nextStep":"UNDER_REVIEW"}
                """);
        when(evidenceAgent.collect(any())).thenReturn(sampleEvidence());

        var response = service.analyze(context);

        assertEquals("FALLBACK_HUMAN", response.getValidationStatus());
        assertEquals("NEEDS_HUMAN", response.getDecision().getAction());
        ArgumentCaptor<AiDecisionRecord> captor = ArgumentCaptor.forClass(AiDecisionRecord.class);
        verify(aiAuditService).saveDecision(captor.capture());
        assertEquals("FALLBACK_HUMAN", captor.getValue().getValidationStatus());
    }

    private EvidenceCollection sampleEvidence() {
        EvidenceCollection collection = new EvidenceCollection();
        Map<String, Object> orderData = new LinkedHashMap<>();
        orderData.put("evidenceId", "order:10001");
        Map<String, Object> logisticsData = new LinkedHashMap<>();
        logisticsData.put("available", true);
        logisticsData.put("evidenceId", "logistics:10001");
        Map<String, Object> riskData = new LinkedHashMap<>();
        riskData.put("riskLevel", "LOW");
        riskData.put("evidenceId", "risk:7");
        collection.getToolResults().add(ToolResult.ok("query_order", "tr-12345678", orderData, 1L));
        collection.getToolResults().add(ToolResult.ok("query_logistics", "tr-12345678", logisticsData, 1L));
        collection.getToolResults().add(ToolResult.ok("query_user_risk", "tr-12345678", riskData, 1L));

        RuleHit hit = new RuleHit();
        hit.setChunkId(11L);
        hit.setChunkNo(1);
        hit.setDocumentCode("AFTER_SALE_LOGISTICS_NOT_RECEIVED");
        hit.setTitle("物流签收未收到处置规则");
        hit.setCategory("LOGISTICS");
        hit.setPolicyVersion("v1");
        hit.setPermissionScope("PUBLIC");
        hit.setContent("物流显示签收但未收到，先人工核实签收地点和驿站取件记录");
        collection.setRuleHits(List.of(hit));
        return collection;
    }
}

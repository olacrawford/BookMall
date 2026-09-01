package com.bookmall.aftersale.service;

import com.bookmall.aftersale.ai.model.AiAnalysisResponse;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleAnalysisQueryServiceTest {

    @Mock
    private AfterSaleOrderMapper orderMapper;
    @Mock
    private AfterSaleTicketMapper ticketMapper;
    @Mock
    private AiDecisionRecordMapper decisionMapper;
    @Mock
    private AiEvidenceMapper evidenceMapper;
    @Mock
    private ToolCallLogMapper toolLogMapper;

    private AfterSaleAnalysisQueryService service() {
        return new AfterSaleAnalysisQueryService(orderMapper, ticketMapper, decisionMapper,
                evidenceMapper, toolLogMapper, new AfterSaleAccessGuard(Set.of(1L)), new ObjectMapper());
    }

    @Test
    void getAnalysis_returnsPersistedDecision_whenOwnerReadsAfterSale() {
        AfterSaleOrder order = new AfterSaleOrder();
        order.setId(1001L);
        order.setUserId(7L);
        when(orderMapper.selectById(1001L)).thenReturn(order);

        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setId(91001L);
        ticket.setAfterSaleId(1001L);
        when(ticketMapper.selectOne(any())).thenReturn(ticket);

        AiDecisionRecord record = new AiDecisionRecord();
        record.setId(6L);
        record.setTicketId(91001L);
        record.setTraceId("tr-phase5-persisted");
        record.setValidationStatus("VALID");
        record.setDecisionJson("""
                {"intent":"LOGISTICS_NOT_RECEIVED","action":"NEEDS_HUMAN","amount":39.80,
                 "reason":"先人工核实","riskLevel":"LOW",
                 "evidenceIds":["order:10001","policy:v1#AFTER_SALE_LOGISTICS_NOT_RECEIVED#1"],
                 "policyVersion":"v1","nextStep":"UNDER_REVIEW"}
                """);
        when(decisionMapper.selectOne(any())).thenReturn(record);

        ToolCallLog toolLog = new ToolCallLog();
        toolLog.setToolName("query_order");
        toolLog.setTraceId("tr-phase5-persisted");
        toolLog.setSuccess(1);
        toolLog.setLatencyMs(5);
        toolLog.setArgumentsJson("{\"orderId\":10001}");
        when(toolLogMapper.selectList(any())).thenReturn(List.of(toolLog));

        AiEvidence evidence = new AiEvidence();
        evidence.setDecisionId(6L);
        evidence.setEvidenceId("order:10001");
        evidence.setEvidenceType("ORDER");
        evidence.setSourceRef("query_order");
        evidence.setContent("{\"orderId\":10001,\"evidenceId\":\"order:10001\"}");
        evidence.setPolicyVersion("v1");
        when(evidenceMapper.selectList(any())).thenReturn(List.of(evidence));

        AiAnalysisResponse response = service().getAnalysis(7L, 1001L);

        assertEquals("VALID", response.getValidationStatus());
        assertEquals("NEEDS_HUMAN", response.getDecision().getAction());
        assertEquals("v1", response.getDecision().getPolicyVersion());
        assertEquals(1, response.getToolResults().size());
        assertEquals("order:10001", response.getToolResults().get(0).getData().get("evidenceId"));
    }

    @Test
    void getAnalysis_rejectsOtherUser() {
        AfterSaleOrder order = new AfterSaleOrder();
        order.setId(1001L);
        order.setUserId(7L);
        when(orderMapper.selectById(1001L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> service().getAnalysis(8L, 1001L));
    }
}

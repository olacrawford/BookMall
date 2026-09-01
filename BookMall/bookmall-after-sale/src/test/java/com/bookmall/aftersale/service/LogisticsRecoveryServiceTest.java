package com.bookmall.aftersale.service;

import com.bookmall.aftersale.entity.AfterSaleOrder;
import com.bookmall.aftersale.entity.AfterSaleTicket;
import com.bookmall.aftersale.entity.WorkflowInstance;
import com.bookmall.aftersale.entity.WorkflowStep;
import com.bookmall.aftersale.mapper.AfterSaleOrderMapper;
import com.bookmall.aftersale.mapper.AfterSaleTicketMapper;
import com.bookmall.aftersale.mapper.WorkflowInstanceMapper;
import com.bookmall.aftersale.mapper.WorkflowStepMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogisticsRecoveryServiceTest {

    @Mock
    private AfterSaleOrderMapper afterSaleOrderMapper;

    @Mock
    private AfterSaleTicketMapper afterSaleTicketMapper;

    @Mock
    private WorkflowInstanceMapper workflowInstanceMapper;

    @Mock
    private WorkflowStepMapper workflowStepMapper;

    @Spy
    private AfterSaleStatusMachine statusMachine = new AfterSaleStatusMachine();

    @InjectMocks
    private LogisticsRecoveryService service;

    @Test
    void degradeToHuman_marksFailedAndWaitingHuman_whenTimeout() {
        AfterSaleOrder order = new AfterSaleOrder();
        order.setId(90001L);
        order.setStatus(AfterSaleStatusMachine.UNDER_REVIEW);
        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setId(92001L);
        WorkflowStep step = new WorkflowStep();
        step.setId(1L);
        step.setWorkflowId(92001L);
        step.setStepKey("LOGISTICS_CHECK");
        step.setStatus("RUNNING");
        when(workflowStepMapper.selectOne(any())).thenReturn(step);

        boolean result = service.degradeToHuman(order, workflow, LocalDateTime.now());

        assertTrue(result);
        assertEquals(AfterSaleStatusMachine.WAITING_HUMAN, order.getStatus());
        assertEquals("FAILED", step.getStatus());
        assertEquals("LOGISTICS_TIMEOUT", step.getLastErrorCode());
        verify(afterSaleOrderMapper).updateById(order);
        verify(workflowStepMapper).updateById(step);
    }

    @Test
    void resumeFromHuman_returnsToProcessingAndCompletesCheck() {
        AfterSaleOrder order = new AfterSaleOrder();
        order.setId(90001L);
        order.setStatus(AfterSaleStatusMachine.WAITING_HUMAN);
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setId(93001L);
        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setId(92001L);
        workflow.setStatus("RUNNING");
        WorkflowStep step = new WorkflowStep();
        step.setId(1L);
        step.setStatus("FAILED");
        when(afterSaleOrderMapper.selectById(90001L)).thenReturn(order);
        when(afterSaleTicketMapper.selectOne(any())).thenReturn(ticket);
        when(workflowInstanceMapper.selectOne(any())).thenReturn(workflow);
        when(workflowStepMapper.selectOne(any())).thenReturn(step);

        boolean result = service.resumeFromHuman(90001L);

        assertTrue(result);
        assertEquals(AfterSaleStatusMachine.PROCESSING, order.getStatus());
        assertEquals("PROCESSING", ticket.getDecisionStatus());
        assertEquals("COMPLETED", step.getStatus());
        assertEquals("PROCESSING", workflow.getCurrentStep());
        verify(afterSaleOrderMapper).updateById(order);
    }
}

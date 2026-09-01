package com.bookmall.aftersale.service;

import com.bookmall.aftersale.entity.WorkflowInstance;
import com.bookmall.aftersale.entity.WorkflowStep;
import com.bookmall.aftersale.mapper.WorkflowInstanceMapper;
import com.bookmall.aftersale.mapper.WorkflowStepMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowRecoveryServiceTest {

    @Mock
    private WorkflowStepMapper workflowStepMapper;

    @Mock
    private WorkflowInstanceMapper workflowInstanceMapper;

    @InjectMocks
    private WorkflowRecoveryService workflowRecoveryService;

    @Test
    void recoverPendingSteps_schedulesRetryWithIncrementedAttempt() {
        WorkflowStep step = new WorkflowStep();
        step.setId(1L);
        step.setWorkflowId(20L);
        step.setStepKey("POLICY_CHECK");
        step.setStatus("FAILED");
        step.setAttemptCount(1);
        when(workflowStepMapper.selectList(any())).thenReturn(List.of(step));

        int recovered = workflowRecoveryService.recoverPendingSteps(10, 30);

        assertEquals(1, recovered);
        assertEquals("RETRYING", step.getStatus());
        assertEquals(2, step.getAttemptCount());
        assertNotNull(step.getNextRetryTime());
        verify(workflowStepMapper, times(1)).updateById(step);
    }

    @Test
    void completeRecovery_marksStepCompletedAndResumesWorkflow() {
        WorkflowStep step = new WorkflowStep();
        step.setId(1L);
        step.setWorkflowId(20L);
        step.setStepKey("POLICY_CHECK");
        step.setStatus("RETRYING");
        when(workflowStepMapper.selectById(1L)).thenReturn(step);

        WorkflowInstance workflow = new WorkflowInstance();
        workflow.setId(20L);
        workflow.setStatus("COMPLETED");
        when(workflowInstanceMapper.selectById(20L)).thenReturn(workflow);

        assertTrue(workflowRecoveryService.completeRecovery(1L));
        assertEquals("COMPLETED", step.getStatus());
        assertEquals("RUNNING", workflow.getStatus());
        assertEquals("POLICY_CHECK", workflow.getCurrentStep());
        verify(workflowInstanceMapper, times(1)).updateById(workflow);
    }
}

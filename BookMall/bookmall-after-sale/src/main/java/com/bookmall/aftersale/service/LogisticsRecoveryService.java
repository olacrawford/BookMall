package com.bookmall.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.aftersale.entity.AfterSaleOrder;
import com.bookmall.aftersale.entity.AfterSaleTicket;
import com.bookmall.aftersale.entity.WorkflowInstance;
import com.bookmall.aftersale.entity.WorkflowStep;
import com.bookmall.aftersale.mapper.AfterSaleOrderMapper;
import com.bookmall.aftersale.mapper.AfterSaleTicketMapper;
import com.bookmall.aftersale.mapper.WorkflowInstanceMapper;
import com.bookmall.aftersale.mapper.WorkflowStepMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogisticsRecoveryService {

    private static final String STEP_KEY = "LOGISTICS_CHECK";

    private static final Logger log = LoggerFactory.getLogger(LogisticsRecoveryService.class);

    private final AfterSaleOrderMapper afterSaleOrderMapper;
    private final AfterSaleTicketMapper afterSaleTicketMapper;
    private final WorkflowInstanceMapper workflowInstanceMapper;
    private final WorkflowStepMapper workflowStepMapper;
    private final AfterSaleStatusMachine statusMachine;

    public LogisticsRecoveryService(AfterSaleOrderMapper afterSaleOrderMapper,
                                    AfterSaleTicketMapper afterSaleTicketMapper,
                                    WorkflowInstanceMapper workflowInstanceMapper,
                                    WorkflowStepMapper workflowStepMapper,
                                    AfterSaleStatusMachine statusMachine) {
        this.afterSaleOrderMapper = afterSaleOrderMapper;
        this.afterSaleTicketMapper = afterSaleTicketMapper;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.workflowStepMapper = workflowStepMapper;
        this.statusMachine = statusMachine;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean degradeToHuman(AfterSaleOrder order, WorkflowInstance workflow, LocalDateTime now) {
        if (order == null || workflow == null) {
            return false;
        }
        String current = order.getStatus();
        if (!AfterSaleStatusMachine.UNDER_REVIEW.equals(current)
                && !AfterSaleStatusMachine.PROCESSING.equals(current)) {
            return false;
        }
        String after = statusMachine.transition(current, AfterSaleStatusMachine.WAITING_HUMAN);
        order.setStatus(after);
        order.setUpdateTime(now);
        afterSaleOrderMapper.updateById(order);

        workflow.setCurrentStep(STEP_KEY);
        workflow.setUpdateTime(now);
        workflowInstanceMapper.updateById(workflow);

        WorkflowStep step = findStep(workflow.getId(), STEP_KEY);
        if (step == null) {
            step = new WorkflowStep();
            step.setWorkflowId(workflow.getId());
            step.setStepKey(STEP_KEY);
            step.setStatus("FAILED");
            step.setAttemptCount(1);
            step.setLastErrorCode("LOGISTICS_TIMEOUT");
            step.setLastErrorMessage("物流查询超时，已转入人工处理");
            step.setVersion(0);
            step.setCreateTime(now);
            step.setUpdateTime(now);
            workflowStepMapper.insert(step);
        } else {
            step.setStatus("FAILED");
            step.setAttemptCount(step.getAttemptCount() == null ? 1 : step.getAttemptCount() + 1);
            step.setLastErrorCode("LOGISTICS_TIMEOUT");
            step.setLastErrorMessage("物流查询超时，已转入人工处理");
            step.setUpdateTime(now);
            workflowStepMapper.updateById(step);
        }
        log.info("after-sale degraded to WAITING_HUMAN afterSaleId={} workflowId={}", order.getId(), workflow.getId());
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeLogisticsCheck(Long workflowId, LocalDateTime now) {
        WorkflowStep step = findStep(workflowId, STEP_KEY);
        if (step == null) {
            return;
        }
        step.setStatus("COMPLETED");
        step.setAttemptCount(step.getAttemptCount() == null ? 1 : step.getAttemptCount());
        step.setLastErrorCode(null);
        step.setLastErrorMessage(null);
        step.setUpdateTime(now);
        workflowStepMapper.updateById(step);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean resumeFromHuman(Long afterSaleId) {
        AfterSaleOrder order = afterSaleOrderMapper.selectById(afterSaleId);
        if (order == null || !AfterSaleStatusMachine.WAITING_HUMAN.equals(order.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(statusMachine.transition(order.getStatus(), AfterSaleStatusMachine.PROCESSING));
        order.setUpdateTime(now);
        afterSaleOrderMapper.updateById(order);

        AfterSaleTicket ticket = afterSaleTicketMapper.selectOne(new LambdaQueryWrapper<AfterSaleTicket>()
                .eq(AfterSaleTicket::getAfterSaleId, afterSaleId));
        if (ticket != null) {
            ticket.setDecisionStatus("PROCESSING");
            ticket.setUpdateTime(now);
            afterSaleTicketMapper.updateById(ticket);
        }
        WorkflowInstance workflow = ticket == null ? null : workflowInstanceMapper.selectOne(
                new LambdaQueryWrapper<WorkflowInstance>()
                        .eq(WorkflowInstance::getTicketId, ticket.getId()));
        if (workflow != null) {
            workflow.setStatus("RUNNING");
            workflow.setCurrentStep("PROCESSING");
            workflow.setUpdateTime(now);
            workflowInstanceMapper.updateById(workflow);
            WorkflowStep step = findStep(workflow.getId(), STEP_KEY);
            if (step != null) {
                step.setStatus("COMPLETED");
                step.setLeaseUntil(null);
                step.setNextRetryTime(null);
                step.setUpdateTime(now);
                workflowStepMapper.updateById(step);
            }
        }
        log.info("after-sale resumed from WAITING_HUMAN afterSaleId={}", afterSaleId);
        return true;
    }

    private WorkflowStep findStep(Long workflowId, String stepKey) {
        return workflowStepMapper.selectOne(new LambdaQueryWrapper<WorkflowStep>()
                .eq(WorkflowStep::getWorkflowId, workflowId)
                .eq(WorkflowStep::getStepKey, stepKey));
    }
}

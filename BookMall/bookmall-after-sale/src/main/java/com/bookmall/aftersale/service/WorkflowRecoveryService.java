package com.bookmall.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.aftersale.entity.WorkflowInstance;
import com.bookmall.aftersale.entity.WorkflowStep;
import com.bookmall.aftersale.mapper.WorkflowInstanceMapper;
import com.bookmall.aftersale.mapper.WorkflowStepMapper;
import com.bookmall.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkflowRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRecoveryService.class);

    private final WorkflowStepMapper workflowStepMapper;
    private final WorkflowInstanceMapper workflowInstanceMapper;

    public WorkflowRecoveryService(WorkflowStepMapper workflowStepMapper,
                                   WorkflowInstanceMapper workflowInstanceMapper) {
        this.workflowStepMapper = workflowStepMapper;
        this.workflowInstanceMapper = workflowInstanceMapper;
    }

    @Scheduled(fixedDelayString = "${after-sale.workflow.recovery.delay-ms:30000}",
            initialDelayString = "${after-sale.workflow.recovery.initial-delay-ms:10000}")
    public void scheduledRecovery() {
        recoverPendingSteps(50, 30);
    }

    @Transactional(rollbackFor = Exception.class)
    public void failStepForRetry(Long stepId, String errorCode, String errorMessage) {
        WorkflowStep step = requireStep(stepId);
        step.setStatus("FAILED");
        step.setLastErrorCode(errorCode);
        step.setLastErrorMessage(errorMessage);
        step.setUpdateTime(LocalDateTime.now());
        workflowStepMapper.updateById(step);
        log.info("workflow step marked failed stepId={} errorCode={}", stepId, errorCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public int recoverPendingSteps(int limit, int retryAfterSeconds) {
        if (limit <= 0) {
            return 0;
        }
        List<WorkflowStep> failed = workflowStepMapper.selectList(new LambdaQueryWrapper<WorkflowStep>()
                .eq(WorkflowStep::getStatus, "FAILED")
                .orderByAsc(WorkflowStep::getCreateTime)
                .last("LIMIT " + limit));
        int recovered = 0;
        LocalDateTime now = LocalDateTime.now();
        for (WorkflowStep step : failed) {
            step.setAttemptCount(step.getAttemptCount() == null ? 1 : step.getAttemptCount() + 1);
            step.setStatus("RETRYING");
            step.setNextRetryTime(now.plusSeconds(retryAfterSeconds));
            step.setLastErrorCode(step.getLastErrorCode());
            step.setUpdateTime(now);
            workflowStepMapper.updateById(step);
            log.info("workflow step queued for retry stepId={} attempt={} nextRetryAt={}",
                    step.getId(), step.getAttemptCount(), step.getNextRetryTime());
            recovered++;
        }
        return recovered;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeRecovery(Long stepId) {
        WorkflowStep step = requireStep(stepId);
        if (!"RETRYING".equals(step.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        step.setStatus("COMPLETED");
        step.setLeaseUntil(null);
        step.setNextRetryTime(null);
        step.setUpdateTime(now);
        workflowStepMapper.updateById(step);

        WorkflowInstance instance = workflowInstanceMapper.selectById(step.getWorkflowId());
        if (instance != null) {
            instance.setStatus("RUNNING");
            instance.setCurrentStep(step.getStepKey());
            instance.setUpdateTime(now);
            workflowInstanceMapper.updateById(instance);
        }
        log.info("workflow step recovery completed stepId={}", stepId);
        return true;
    }

    private WorkflowStep requireStep(Long stepId) {
        WorkflowStep step = workflowStepMapper.selectById(stepId);
        if (step == null) {
            throw new BusinessException(404, "工作流步骤不存在");
        }
        return step;
    }
}

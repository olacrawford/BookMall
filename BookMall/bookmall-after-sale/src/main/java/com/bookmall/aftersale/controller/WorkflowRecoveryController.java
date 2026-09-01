package com.bookmall.aftersale.controller;

import com.bookmall.aftersale.service.WorkflowRecoveryService;
import com.bookmall.common.result.Result;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/workflow")
public class WorkflowRecoveryController {

    private final WorkflowRecoveryService workflowRecoveryService;

    public WorkflowRecoveryController(WorkflowRecoveryService workflowRecoveryService) {
        this.workflowRecoveryService = workflowRecoveryService;
    }

    @PostMapping("/recover")
    public Result<Integer> recover(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.success(workflowRecoveryService.recoverPendingSteps(limit, 30));
    }

    @PostMapping("/steps/{stepId}/fail")
    public Result<Void> failStep(@PathVariable Long stepId,
                                 @RequestParam(value = "errorCode", defaultValue = "WORKER_INTERRUPTED") String errorCode,
                                 @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        workflowRecoveryService.failStepForRetry(stepId, errorCode, errorMessage);
        return Result.success();
    }

    @PostMapping("/steps/{stepId}/complete")
    public Result<Boolean> complete(@PathVariable Long stepId) {
        return Result.success(workflowRecoveryService.completeRecovery(stepId));
    }
}

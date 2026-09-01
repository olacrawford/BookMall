package com.bookmall.aftersale.controller;

import com.bookmall.aftersale.dto.ApprovalRequest;
import com.bookmall.aftersale.service.AfterSaleAccessGuard;
import com.bookmall.aftersale.service.AfterSaleService;
import com.bookmall.aftersale.vo.ApprovalQueueVO;
import com.bookmall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/approval-tasks")
public class ApprovalTaskController {

    private final AfterSaleService afterSaleService;
    private final AfterSaleAccessGuard accessGuard;

    public ApprovalTaskController(AfterSaleService afterSaleService, AfterSaleAccessGuard accessGuard) {
        this.afterSaleService = afterSaleService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public Result<ApprovalQueueVO> list(@RequestHeader("X-User-Id") Long operatorId,
                                        @RequestHeader(value = "X-User-Roles", required = false) String operatorRoles,
                                        @RequestParam(value = "status", required = false) String status) {
        accessGuard.requireApprover(operatorId, operatorRoles);
        return Result.success(afterSaleService.listApprovalTasks(status));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@RequestHeader("X-User-Id") Long operatorId,
                                        @RequestHeader(value = "X-User-Roles", required = false) String operatorRoles,
                                @PathVariable Long id,
                                @RequestBody ApprovalRequest request) {
        accessGuard.requireApprover(operatorId, operatorRoles);
        afterSaleService.approve(operatorId, id, request);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@RequestHeader("X-User-Id") Long operatorId,
                                        @RequestHeader(value = "X-User-Roles", required = false) String operatorRoles,
                               @PathVariable Long id,
                               @RequestBody ApprovalRequest request) {
        accessGuard.requireApprover(operatorId, operatorRoles);
        afterSaleService.reject(operatorId, id, request);
        return Result.success();
    }
}

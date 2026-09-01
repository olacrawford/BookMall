package com.bookmall.aftersale.controller;

import com.bookmall.aftersale.service.LogisticsRecoveryService;
import com.bookmall.common.result.Result;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/logistics")
public class LogisticsRecoveryController {

    private final LogisticsRecoveryService logisticsRecoveryService;

    public LogisticsRecoveryController(LogisticsRecoveryService logisticsRecoveryService) {
        this.logisticsRecoveryService = logisticsRecoveryService;
    }

    @PostMapping("/{afterSaleId}/recover")
    public Result<Boolean> recover(@PathVariable Long afterSaleId) {
        return Result.success(logisticsRecoveryService.resumeFromHuman(afterSaleId));
    }
}

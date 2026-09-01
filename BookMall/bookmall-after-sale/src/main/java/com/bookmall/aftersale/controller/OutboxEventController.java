package com.bookmall.aftersale.controller;

import com.bookmall.aftersale.service.AfterSaleOutboxService;
import com.bookmall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/outbox")
public class OutboxEventController {

    private final AfterSaleOutboxService outboxService;

    public OutboxEventController(AfterSaleOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @GetMapping("/scan")
    public Result<Integer> scan(@RequestParam(value = "limit", defaultValue = "10") int limit) {
        return Result.success(outboxService.scanAndDispatch(limit));
    }

    @PostMapping("/{eventId}/consume")
    public Result<Boolean> consume(@PathVariable String eventId) {
        return Result.success(outboxService.consume(eventId));
    }
}

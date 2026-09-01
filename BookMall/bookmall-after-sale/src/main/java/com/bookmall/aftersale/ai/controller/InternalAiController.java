package com.bookmall.aftersale.ai.controller;

import com.bookmall.aftersale.ai.dto.AnalyzeRequest;
import com.bookmall.aftersale.ai.model.AiAnalysisResponse;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.service.AfterSaleAiAnalysisService;
import com.bookmall.aftersale.ai.tool.DomainToolRegistry;
import com.bookmall.common.exception.BusinessException;
import com.bookmall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/internal/ai")
public class InternalAiController {

    private final AfterSaleAiAnalysisService analysisService;
    private final DomainToolRegistry toolRegistry;

    public InternalAiController(AfterSaleAiAnalysisService analysisService, DomainToolRegistry toolRegistry) {
        this.analysisService = analysisService;
        this.toolRegistry = toolRegistry;
    }

    @PostMapping("/analyze")
    public Result<AiAnalysisResponse> analyze(@RequestBody AnalyzeRequest request) {
        if (request == null || request.getTicketId() == null || request.getDescription() == null
                || request.getDescription().isBlank()) {
            throw new BusinessException(422, "ticketId and description are required");
        }
        TicketContext context = new TicketContext();
        context.setTraceId(request.getTraceId());
        context.setTicketId(request.getTicketId());
        context.setUserId(request.getUserId());
        context.setOrderId(request.getOrderId());
        context.setDescription(request.getDescription());
        context.setOrderSnapshot(request.getOrderSnapshot());
        context.setPolicyVersion(request.getPolicyVersion());
        context.setUserEvidence(request.getUserEvidence());
        return Result.success(analysisService.analyze(context));
    }

    @GetMapping("/tools")
    public Result<Set<String>> tools() {
        return Result.success(toolRegistry.allowedTools());
    }
}

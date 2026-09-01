package com.bookmall.aftersale.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiAnalysisResponse {

    private String traceId;
    private Long ticketId;
    private String intent;
    private AiDecision decision;
    private String validationStatus;
    private List<ToolResult> toolResults = new ArrayList<>();
    private List<RuleHit> ruleHits = new ArrayList<>();
    private List<String> validationErrors = new ArrayList<>();
}

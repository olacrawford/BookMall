package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.TicketContext;
import com.bookmall.aftersale.ai.model.ToolResult;

import java.util.List;

public interface LlmClient {

    String generateDecision(TicketContext context, String intent, List<ToolResult> toolResults, List<RuleHit> ruleHits);

    String provider();

    String modelName();
}

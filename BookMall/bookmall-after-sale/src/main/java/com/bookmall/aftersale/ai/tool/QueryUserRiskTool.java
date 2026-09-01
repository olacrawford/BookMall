package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QueryUserRiskTool implements DomainTool {

    @Override
    public String name() {
        return "query_user_risk";
    }

    @Override
    public long timeoutMs() {
        return 200;
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation) {
        Long userId = ToolArgumentReader.longValue(invocation.getArguments(), "userId");
        String hint = ToolArgumentReader.stringValue(invocation.getArguments(), "riskHint");
        String riskLevel = hint == null || hint.isBlank() ? "LOW" : hint.toUpperCase();
        if (!"LOW".equals(riskLevel) && !"MEDIUM".equals(riskLevel) && !"HIGH".equals(riskLevel)) {
            riskLevel = "LOW";
        }
        BigDecimal score = "HIGH".equals(riskLevel)
                ? BigDecimal.valueOf(0.85)
                : "MEDIUM".equals(riskLevel) ? BigDecimal.valueOf(0.5) : BigDecimal.valueOf(0.1);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("riskLevel", riskLevel);
        data.put("score", score);
        data.put("evidenceId", "risk:" + (userId == null ? "unknown" : userId));
        return ToolResult.ok(name(), invocation.getTraceId(), data, 0L);
    }
}

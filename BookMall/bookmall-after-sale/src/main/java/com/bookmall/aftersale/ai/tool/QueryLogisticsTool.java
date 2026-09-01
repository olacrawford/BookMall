package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.client.LogisticsQueryGateway;
import com.bookmall.aftersale.client.LogisticsQueryResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QueryLogisticsTool implements DomainTool {

    private final LogisticsQueryGateway logisticsQueryGateway;

    public QueryLogisticsTool(LogisticsQueryGateway logisticsQueryGateway) {
        this.logisticsQueryGateway = logisticsQueryGateway;
    }

    @Override
    public String name() {
        return "query_logistics";
    }

    @Override
    public long timeoutMs() {
        return 800;
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation) {
        Long orderId = ToolArgumentReader.longValue(invocation.getArguments(), "orderId");
        Long afterSaleId = ToolArgumentReader.longValue(invocation.getArguments(), "afterSaleId");
        if (orderId == null) {
            return ToolResult.fail(name(), invocation.getTraceId(), "INVALID_ARGUMENTS", 0L);
        }
        LogisticsQueryResult result = logisticsQueryGateway.query(afterSaleId, orderId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId);
        data.put("available", result.available());
        data.put("timeout", result.timeout());
        data.put("reason", result.reason());
        data.put("evidenceId", "logistics:" + orderId);
        if (!result.available()) {
            return new ToolResult(name(), false, invocation.getTraceId(), 0L, "LOGISTICS_UNAVAILABLE", data, invocation.getArguments());
        }
        return ToolResult.ok(name(), invocation.getTraceId(), data, 0L);
    }
}

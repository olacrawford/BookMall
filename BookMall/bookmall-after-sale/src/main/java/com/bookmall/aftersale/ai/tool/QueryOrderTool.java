package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.client.OrderClient;
import com.bookmall.aftersale.client.dto.OrderSnapshot;
import com.bookmall.common.result.Result;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class QueryOrderTool implements DomainTool {

    private final OrderClient orderClient;

    public QueryOrderTool(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @Override
    public String name() {
        return "query_order";
    }

    @Override
    public long timeoutMs() {
        return 800;
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation) {
        Long orderId = ToolArgumentReader.longValue(invocation.getArguments(), "orderId");
        Long userId = ToolArgumentReader.longValue(invocation.getArguments(), "userId");
        if (orderId == null || userId == null) {
            return ToolResult.fail(name(), invocation.getTraceId(), "INVALID_ARGUMENTS", 0L);
        }
        try {
            Result<OrderSnapshot> result = orderClient.getOrderDetail(orderId, userId);
            if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
                return ToolResult.fail(name(), invocation.getTraceId(), "ORDER_NOT_FOUND", 0L);
            }
            OrderSnapshot snapshot = result.getData();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderId", snapshot.getId());
            data.put("orderNo", snapshot.getOrderNo());
            data.put("userId", snapshot.getUserId());
            data.put("amount", snapshot.getTotalAmount());
            data.put("status", snapshot.getStatus());
            data.put("evidenceId", "order:" + snapshot.getId());
            return ToolResult.ok(name(), invocation.getTraceId(), data, 0L);
        } catch (Exception ex) {
            return ToolResult.fail(name(), invocation.getTraceId(), "ORDER_SERVICE_UNAVAILABLE", 0L);
        }
    }
}

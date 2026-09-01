package com.bookmall.aftersale.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {

    private String toolName;
    private boolean success;
    private String traceId;
    private Long latencyMs;
    private String errorCode;
    private Map<String, Object> data = new HashMap<>();
    private Map<String, Object> arguments = new HashMap<>();

    public static ToolResult ok(String toolName, String traceId, Map<String, Object> data, long latencyMs) {
        return new ToolResult(toolName, true, traceId, latencyMs, null, data, new HashMap<>());
    }

    public static ToolResult fail(String toolName, String traceId, String errorCode, long latencyMs) {
        return new ToolResult(toolName, false, traceId, latencyMs, errorCode, new HashMap<>(), new HashMap<>());
    }
}

package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainToolRegistryTest {

    @Test
    void invoke_returnsTimeout_whenToolExceedsDeadline() {
        DomainTool slowTool = new StubTool("slow", 20, false, 100);
        DomainToolRegistry registry = new DomainToolRegistry(List.of(slowTool));

        ToolResult result = registry.invoke(invocation("slow", Map.of("key", "value")));

        assertFalse(result.isSuccess());
        assertEquals("TOOL_TIMEOUT", result.getErrorCode());
    }

    @Test
    void invoke_unknownTool_rejectsBeforeExecution() {
        DomainToolRegistry registry = new DomainToolRegistry(List.of(new StubTool("allowed", 100, true, 0)));

        ToolResult result = registry.invoke(invocation("unknown", Map.of("key", "value")));

        assertFalse(result.isSuccess());
        assertEquals("UNKNOWN_TOOL", result.getErrorCode());
    }

    @Test
    void invoke_okResult_preservesArgumentsForAudit() {
        StubTool tool = new StubTool("allowed", 100, true, 0);
        DomainToolRegistry registry = new DomainToolRegistry(List.of(tool));
        ToolInvocation invocation = invocation("allowed", Map.of("orderId", 10001L));

        ToolResult result = registry.invoke(invocation);

        assertTrue(result.isSuccess());
        assertEquals(invocation.getArguments(), result.getArguments());
    }

    private ToolInvocation invocation(String toolName, Map<String, Object> arguments) {
        ToolInvocation invocation = new ToolInvocation();
        invocation.setToolName(toolName);
        invocation.setTraceId("tr-12345678");
        invocation.setTicketId(91001L);
        invocation.setArguments(new LinkedHashMap<>(arguments));
        return invocation;
    }

    private static final class StubTool implements DomainTool {

        private final String name;
        private final long timeoutMs;
        private final boolean success;
        private final long sleepMs;

        private StubTool(String name, long timeoutMs, boolean success, long sleepMs) {
            this.name = name;
            this.timeoutMs = timeoutMs;
            this.success = success;
            this.sleepMs = sleepMs;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long timeoutMs() {
            return timeoutMs;
        }

        @Override
        public ToolResult invoke(ToolInvocation invocation) {
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            if (success) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("result", "ok");
                return ToolResult.ok(name, invocation.getTraceId(), data, 1L);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("result", "unavailable");
            return new ToolResult(name, false, invocation.getTraceId(), 1L, "FAIL", data, invocation.getArguments());
        }
    }
}

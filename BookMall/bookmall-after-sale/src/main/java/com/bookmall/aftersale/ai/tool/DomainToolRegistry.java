package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DomainToolRegistry {

    private final Map<String, DomainTool> tools;

    public DomainToolRegistry(List<DomainTool> toolList) {
        this.tools = toolList.stream().collect(Collectors.toUnmodifiableMap(DomainTool::name, Function.identity()));
    }

    public Set<String> allowedTools() {
        return tools.keySet();
    }

    public ToolResult invoke(ToolInvocation invocation) {
        ToolResult invalid = validateInvocation(invocation);
        if (invalid != null) {
            return invalid;
        }
        DomainTool tool = tools.get(invocation.getToolName());
        try {
            ToolResult result = CompletableFuture.supplyAsync(() -> tool.invoke(invocation))
                    .get(tool.timeoutMs(), TimeUnit.MILLISECONDS);
            if (result != null) {
                result.setArguments(invocation.getArguments());
            }
            return result;
        } catch (TimeoutException ex) {
            return ToolResult.fail(invocation.getToolName(), invocation.getTraceId(), "TOOL_TIMEOUT", tool.timeoutMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ToolResult.fail(invocation.getToolName(), invocation.getTraceId(), "TOOL_INTERRUPTED", 0L);
        } catch (ExecutionException ex) {
            return ToolResult.fail(invocation.getToolName(), invocation.getTraceId(), "TOOL_EXECUTION_ERROR", 0L);
        }
    }

    private ToolResult validateInvocation(ToolInvocation invocation) {
        if (invocation == null || invocation.getToolName() == null || !tools.containsKey(invocation.getToolName())) {
            return ToolResult.fail(invocation == null ? "unknown" : invocation.getToolName(),
                    invocation == null ? "unknown" : invocation.getTraceId(), "UNKNOWN_TOOL", 0L);
        }
        if (invocation.getTraceId() == null || invocation.getTraceId().length() < 8) {
            return ToolResult.fail(invocation.getToolName(), invocation.getTraceId(), "TRACE_ID_TOO_SHORT", 0L);
        }
        if (invocation.getTicketId() == null || invocation.getTicketId() <= 0) {
            return ToolResult.fail(invocation.getToolName(), invocation.getTraceId(), "INVALID_TICKET_ID", 0L);
        }
        if (invocation.getArguments() == null || invocation.getArguments().isEmpty()) {
            return ToolResult.fail(invocation.getToolName(), invocation.getTraceId(), "EMPTY_ARGUMENTS", 0L);
        }
        return null;
    }
}

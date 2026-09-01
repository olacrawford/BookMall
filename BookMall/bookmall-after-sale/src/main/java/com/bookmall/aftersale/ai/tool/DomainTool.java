package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;

public interface DomainTool {

    String name();

    long timeoutMs();

    ToolResult invoke(ToolInvocation invocation);
}

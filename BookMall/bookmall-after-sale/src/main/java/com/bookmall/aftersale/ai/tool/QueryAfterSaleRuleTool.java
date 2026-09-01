package com.bookmall.aftersale.ai.tool;

import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.model.ToolInvocation;
import com.bookmall.aftersale.ai.model.ToolResult;
import com.bookmall.aftersale.ai.rag.RagRetriever;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryAfterSaleRuleTool implements DomainTool {

    private final RagRetriever ragRetriever;

    public QueryAfterSaleRuleTool(RagRetriever ragRetriever) {
        this.ragRetriever = ragRetriever;
    }

    @Override
    public String name() {
        return "query_after_sale_rule";
    }

    @Override
    public long timeoutMs() {
        return 500;
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation) {
        String query = ToolArgumentReader.stringValue(invocation.getArguments(), "query");
        String policyVersion = ToolArgumentReader.stringValue(invocation.getArguments(), "policyVersion");
        String permissionScope = ToolArgumentReader.stringValue(invocation.getArguments(), "permissionScope");
        Long limit = ToolArgumentReader.longValue(invocation.getArguments(), "limit");
        if (query == null || query.isBlank() || policyVersion == null) {
            return ToolResult.fail(name(), invocation.getTraceId(), "INVALID_ARGUMENTS", 0L);
        }
        try {
            List<RuleHit> hits = ragRetriever.search(query, policyVersion,
                    permissionScope == null ? "PUBLIC" : permissionScope,
                    limit == null ? 3 : limit.intValue());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("policyVersion", policyVersion);
            data.put("limit", limit == null ? 3 : limit.intValue());
            data.put("hits", hits.stream().map(this::toMap).toList());
            return ToolResult.ok(name(), invocation.getTraceId(), data, 0L);
        } catch (Exception ex) {
            return ToolResult.fail(name(), invocation.getTraceId(), "RULE_RETRIEVAL_FAILED", 0L);
        }
    }

    private Map<String, Object> toMap(RuleHit hit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("chunkId", hit.getChunkId());
        map.put("documentCode", hit.getDocumentCode());
        map.put("title", hit.getTitle());
        map.put("category", hit.getCategory());
        map.put("chunkNo", hit.getChunkNo());
        map.put("content", hit.getContent());
        map.put("score", hit.getScore());
        map.put("evidenceId", hit.evidenceId());
        return map;
    }
}

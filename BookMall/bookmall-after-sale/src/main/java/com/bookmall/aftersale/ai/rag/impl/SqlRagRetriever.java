package com.bookmall.aftersale.ai.rag.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.ai.rag.RagRetriever;
import com.bookmall.aftersale.entity.RagChunk;
import com.bookmall.aftersale.entity.RagDocument;
import com.bookmall.aftersale.mapper.RagChunkMapper;
import com.bookmall.aftersale.mapper.RagDocumentMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SqlRagRetriever implements RagRetriever {

    private final RagDocumentMapper documentMapper;
    private final RagChunkMapper chunkMapper;

    public SqlRagRetriever(RagDocumentMapper documentMapper, RagChunkMapper chunkMapper) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
    }

    @Override
    public List<RuleHit> search(String query, String policyVersion, String permissionScope, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<RagDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getStatus, "ACTIVE")
                .eq(RagDocument::getPolicyVersion, policyVersion)
                .eq(hasText(permissionScope), RagDocument::getPermissionScope, permissionScope));
        if (documents.isEmpty()) {
            return List.of();
        }
        Map<Long, RagDocument> documentById = new HashMap<>();
        documents.forEach(doc -> documentById.put(doc.getId(), doc));
        List<RagChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<RagChunk>()
                .in(RagChunk::getDocumentId, documentById.keySet()));

        List<RuleHit> hits = new ArrayList<>();
        for (RagChunk chunk : chunks) {
            RuleHit hit = toHit(chunk, documentById.get(chunk.getDocumentId()), query);
            if (hit.getScore().compareTo(BigDecimal.ZERO) > 0) {
                hits.add(hit);
            }
        }
        return hits.stream()
                .sorted(Comparator.comparing(RuleHit::getScore).reversed())
                .limit(limit <= 0 ? 3 : limit)
                .toList();
    }

    private RuleHit toHit(RagChunk chunk, RagDocument document, String query) {
        RuleHit hit = new RuleHit();
        hit.setChunkId(chunk.getId());
        hit.setChunkNo(chunk.getChunkNo());
        hit.setContent(chunk.getContent());
        hit.setDocumentCode(document.getDocumentCode());
        hit.setTitle(document.getTitle());
        hit.setCategory(document.getCategory());
        hit.setPolicyVersion(document.getPolicyVersion());
        hit.setPermissionScope(document.getPermissionScope());
        hit.setScore(score(chunk, query));
        return hit;
    }

    private BigDecimal score(RagChunk chunk, String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        String content = chunk.getContent() == null ? "" : chunk.getContent().toLowerCase(Locale.ROOT);
        String line = content.replaceAll("\\s+", "");
        String queryLine = lowerQuery.replaceAll("\\s+", "");
        double score = 0.0;
        int from = 0;
        while ((from = line.indexOf(queryLine, from)) >= 0) {
            score += 1.0;
            from += queryLine.length();
        }
        if (chunk.getKeywords() != null) {
            for (String keyword : chunk.getKeywords().split(",")) {
                String term = keyword.trim();
                if (!term.isEmpty() && lowerQuery.contains(term.toLowerCase(Locale.ROOT))) {
                    score += 0.6;
                }
            }
        }
        return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

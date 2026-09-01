package com.bookmall.aftersale.ai.rag.impl;

import com.bookmall.aftersale.ai.model.RuleHit;
import com.bookmall.aftersale.entity.RagChunk;
import com.bookmall.aftersale.entity.RagDocument;
import com.bookmall.aftersale.mapper.RagChunkMapper;
import com.bookmall.aftersale.mapper.RagDocumentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlRagRetrieverTest {

    @Mock
    private RagDocumentMapper documentMapper;

    @Mock
    private RagChunkMapper chunkMapper;

    @Test
    void search_logisticsQuestion_returnsVersionedChunkWithScore() {
        RagDocument document = new RagDocument();
        document.setId(1L);
        document.setDocumentCode("AFTER_SALE_LOGISTICS_NOT_RECEIVED");
        document.setTitle("物流签收未收到处置规则");
        document.setCategory("LOGISTICS");
        document.setPolicyVersion("v1");
        document.setPermissionScope("PUBLIC");
        document.setStatus("ACTIVE");

        RagChunk chunk = new RagChunk();
        chunk.setId(11L);
        chunk.setDocumentId(1L);
        chunk.setChunkNo(1);
        chunk.setContent("物流显示签收但未收到，先人工核实签收地点和驿站取件记录");
        chunk.setKeywords("签收,未收到,人工核实");

        when(documentMapper.selectList(any())).thenReturn(List.of(document));
        when(chunkMapper.selectList(any())).thenReturn(List.of(chunk));

        SqlRagRetriever retriever = new SqlRagRetriever(documentMapper, chunkMapper);
        List<RuleHit> hits = retriever.search("物流签收未收到", "v1", "PUBLIC", 3);

        assertEquals(1, hits.size());
        assertEquals("v1", hits.get(0).getPolicyVersion());
        assertEquals(11L, hits.get(0).getChunkId());
        assertEquals("AFTER_SALE_LOGISTICS_NOT_RECEIVED", hits.get(0).getDocumentCode());
        assertEquals("policy:v1#AFTER_SALE_LOGISTICS_NOT_RECEIVED#1", hits.get(0).evidenceId());
        assertTrue(hits.get(0).getScore().compareTo(java.math.BigDecimal.ZERO) > 0);
    }

    @Test
    void search_blankQuery_returnsEmpty() {
        SqlRagRetriever retriever = new SqlRagRetriever(documentMapper, chunkMapper);

        assertTrue(retriever.search("", "v1", "PUBLIC", 3).isEmpty());
    }
}

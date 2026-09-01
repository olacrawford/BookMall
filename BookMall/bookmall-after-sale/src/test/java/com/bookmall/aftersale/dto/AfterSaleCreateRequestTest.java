package com.bookmall.aftersale.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AfterSaleCreateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void evidence_acceptsDocumentedArray() throws Exception {
        AfterSaleCreateRequest request = objectMapper.readValue("""
                {"orderId":10001,"type":"LOGISTICS_NOT_RECEIVED",
                 "description":"未收到包裹","evidence":["门卫没有包裹","驿站无取件记录"]}
                """, AfterSaleCreateRequest.class);

        assertEquals(List.of("门卫没有包裹", "驿站无取件记录"), request.getEvidence());
    }

    @Test
    void evidence_acceptsLegacySingleString() throws Exception {
        AfterSaleCreateRequest request = objectMapper.readValue("""
                {"orderId":10001,"type":"LOGISTICS_NOT_RECEIVED",
                 "description":"未收到包裹","evidence":"门卫没有包裹"}
                """, AfterSaleCreateRequest.class);

        assertEquals(List.of("门卫没有包裹"), request.getEvidence());
    }
}

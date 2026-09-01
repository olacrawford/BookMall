package com.bookmall.aftersale.ai.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RuleHitTest {

    @Test
    void evidenceId_includesDocumentCode_whenChunkNumbersCollide() {
        RuleHit logistics = new RuleHit();
        logistics.setPolicyVersion("v1");
        logistics.setDocumentCode("AFTER_SALE_LOGISTICS_NOT_RECEIVED");
        logistics.setChunkNo(1);

        RuleHit damaged = new RuleHit();
        damaged.setPolicyVersion("v1");
        damaged.setDocumentCode("AFTER_SALE_DAMAGED");
        damaged.setChunkNo(1);

        assertEquals("policy:v1#AFTER_SALE_LOGISTICS_NOT_RECEIVED#1", logistics.evidenceId());
        assertEquals("policy:v1#AFTER_SALE_DAMAGED#1", damaged.evidenceId());
        assertNotEquals(logistics.evidenceId(), damaged.evidenceId());
    }
}

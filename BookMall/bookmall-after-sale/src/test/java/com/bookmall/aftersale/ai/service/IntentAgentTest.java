package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.TicketContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentAgentTest {

    private final IntentAgent intentAgent = new IntentAgent();

    @Test
    void extract_logisticsNotReceived_whenSignedButNotReceived() {
        TicketContext context = new TicketContext();
        context.setDescription("物流显示签收但我没收到包裹");

        assertEquals("LOGISTICS_NOT_RECEIVED", intentAgent.extract(context));
    }

    @Test
    void extract_damaged_whenUserReportsBreakage() {
        TicketContext context = new TicketContext();
        context.setDescription("收到时盒子破损");

        assertEquals("DAMAGED", intentAgent.extract(context));
    }

    @Test
    void extract_generalInquiry_whenNoKnownIntent() {
        TicketContext context = new TicketContext();
        context.setDescription("我想问一下多久能处理");

        assertEquals("GENERAL_INQUIRY", intentAgent.extract(context));
    }
}

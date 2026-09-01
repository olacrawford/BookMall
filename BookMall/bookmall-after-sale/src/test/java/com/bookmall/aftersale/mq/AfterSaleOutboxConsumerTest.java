package com.bookmall.aftersale.mq;

import com.bookmall.aftersale.service.AfterSaleOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleOutboxConsumerTest {

    @Test
    void onRefundExecuted_consumesByIdempotentEventId() throws Exception {
        AfterSaleOutboxService outboxService = mock(AfterSaleOutboxService.class);
        AfterSaleOutboxConsumer consumer = new AfterSaleOutboxConsumer(new ObjectMapper(), outboxService);
        when(outboxService.consume("REFUND_EXECUTED-90001-RF-1")).thenReturn(true);

        consumer.onRefundExecuted("{\"eventId\":\"REFUND_EXECUTED-90001-RF-1\",\"eventType\":\"REFUND_EXECUTED\",\"payload\":\"{}\"}");

        verify(outboxService).consume("REFUND_EXECUTED-90001-RF-1");
    }
}

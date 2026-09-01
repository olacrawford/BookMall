package com.bookmall.aftersale.service;

import com.bookmall.aftersale.entity.AfterSaleOutbox;
import com.bookmall.aftersale.mapper.AfterSaleOutboxMapper;
import com.bookmall.aftersale.mq.AfterSaleOutboxPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleOutboxServiceTest {

    @Mock
    private AfterSaleOutboxMapper outboxMapper;

    @Mock
    private AfterSaleOutboxPublisher outboxPublisher;

    @InjectMocks
    private AfterSaleOutboxService outboxService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void recordRefundExecuted_insertsOutbox() {
        when(outboxMapper.selectOne(any())).thenReturn(null);

        String eventId = outboxService.recordRefundExecuted(90001L, "RF-1", new BigDecimal("39.00"));

        assertEquals("REFUND_EXECUTED-90001-RF-1", eventId);
        verify(outboxMapper, times(1)).insert(any(AfterSaleOutbox.class));
    }

    @Test
    void consume_sameEventTwice_updatesOnlyOnce() {
        AfterSaleOutbox event = new AfterSaleOutbox();
        event.setEventId("REFUND_EXECUTED-90001-RF-1");
        event.setStatus("DISPATCHED");
        when(outboxMapper.selectOne(any())).thenReturn(event);

        assertTrue(outboxService.consume(event.getEventId()));
        assertTrue(outboxService.consume(event.getEventId()));

        verify(outboxMapper, times(1)).updateById(event);
    }

    @Test
    void scanAndDispatch_publishesBeforeMarkingDispatched() {
        AfterSaleOutbox event = new AfterSaleOutbox();
        event.setId(1L);
        event.setEventId("REFUND_EXECUTED-90001-RF-1");
        event.setStatus("CREATED");
        when(outboxMapper.selectList(any())).thenReturn(List.of(event));

        int dispatched = outboxService.scanAndDispatch(10);

        assertEquals(1, dispatched);
        assertEquals("DISPATCHED", event.getStatus());
        verify(outboxPublisher).publish(event);
        verify(outboxMapper).updateById(event);
    }
}

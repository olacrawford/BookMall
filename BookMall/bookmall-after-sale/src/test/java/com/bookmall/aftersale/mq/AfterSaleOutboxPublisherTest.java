package com.bookmall.aftersale.mq;

import com.bookmall.aftersale.dto.OutboxDeliveryMessage;
import com.bookmall.aftersale.entity.AfterSaleOutbox;
import com.bookmall.common.mq.BookMallRabbitMq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AfterSaleOutboxPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    @Test
    void publish_sendsRefundEventsToAfterSaleQueue() {
        AfterSaleOutboxPublisher publisher = new AfterSaleOutboxPublisher(rabbitTemplate, new ObjectMapper());
        AfterSaleOutbox event = new AfterSaleOutbox();
        event.setEventId("REFUND_EXECUTED-90001-RF-1");
        event.setEventType("REFUND_EXECUTED");
        event.setPayload("{\"afterSaleId\":90001}");

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(eq(BookMallRabbitMq.AFTER_SALE_EVENT_EXCHANGE),
                eq(BookMallRabbitMq.AFTER_SALE_REFUND_ROUTING_KEY), anyString());
    }
}

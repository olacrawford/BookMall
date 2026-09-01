package com.bookmall.aftersale.mq;

import com.bookmall.aftersale.dto.OutboxDeliveryMessage;
import com.bookmall.aftersale.entity.AfterSaleOutbox;
import com.bookmall.common.exception.BusinessException;
import com.bookmall.common.mq.BookMallRabbitMq;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AfterSaleOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleOutboxPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public AfterSaleOutboxPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(AfterSaleOutbox event) {
        OutboxDeliveryMessage message = new OutboxDeliveryMessage(
                event.getEventId(), event.getEventType(), event.getPayload());
        try {
            String payload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(BookMallRabbitMq.AFTER_SALE_EVENT_EXCHANGE,
                    BookMallRabbitMq.AFTER_SALE_REFUND_ROUTING_KEY, payload);
            log.info("outbox message published eventId={}", event.getEventId());
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "Outbox 消息序列化失败");
        }
    }
}

package com.bookmall.aftersale.mq;

import com.bookmall.aftersale.dto.OutboxDeliveryMessage;
import com.bookmall.aftersale.service.AfterSaleOutboxService;
import com.bookmall.common.mq.BookMallRabbitMq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AfterSaleOutboxConsumer {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleOutboxConsumer.class);

    private final ObjectMapper objectMapper;
    private final AfterSaleOutboxService outboxService;

    public AfterSaleOutboxConsumer(ObjectMapper objectMapper, AfterSaleOutboxService outboxService) {
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }

    @RabbitListener(queues = BookMallRabbitMq.AFTER_SALE_REFUND_QUEUE)
    public void onRefundExecuted(String payload) {
        try {
            OutboxDeliveryMessage message = objectMapper.readValue(payload, OutboxDeliveryMessage.class);
            boolean consumed = outboxService.consume(message.eventId());
            if (!consumed) {
                log.warn("outbox consume rejected eventId={}", message.eventId());
            } else {
                log.info("outbox message consumed eventId={}", message.eventId());
            }
        } catch (Exception ex) {
            log.error("outbox consume failed payload={}", payload, ex);
        }
    }
}

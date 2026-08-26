package com.bookmall.payment.mq;

import com.bookmall.common.mq.BookMallRabbitMq;
import com.bookmall.common.mq.PaySuccessMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 支付成功事件发布器：同步 Feign 更新订单作为降级，
 * RabbitMQ 消息作为最终一致性补偿通道。
 */
@Slf4j
@Component
public class PaySuccessPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PaySuccessPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(PaySuccessMessage message) throws Exception {
        String payload = objectMapper.writeValueAsString(message);
        rabbitTemplate.convertAndSend(
                BookMallRabbitMq.PAY_SUCCESS_EXCHANGE,
                BookMallRabbitMq.PAY_SUCCESS_ROUTING_KEY,
                payload);
    }
}

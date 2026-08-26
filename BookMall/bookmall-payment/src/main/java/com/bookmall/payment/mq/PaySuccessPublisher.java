package com.bookmall.payment.mq;

import com.bookmall.common.mq.BookMallRabbitMq;
import com.bookmall.common.mq.PaySuccessMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 支付成功事件发布器，订单服务消费后异步更新订单状态。
 */
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

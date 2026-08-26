package com.bookmall.order.mq;

import com.bookmall.common.mq.BookMallRabbitMq;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单服务启用 RabbitMQ 监听，并声明支付成功队列和绑定。
 */
@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public TopicExchange paySuccessExchange() {
        return new TopicExchange(BookMallRabbitMq.PAY_SUCCESS_EXCHANGE, true, false);
    }

    @Bean
    public Queue paySuccessQueue() {
        return new Queue(BookMallRabbitMq.PAY_SUCCESS_QUEUE, true);
    }

    @Bean
    public Binding paySuccessBinding(TopicExchange paySuccessExchange, Queue paySuccessQueue) {
        return BindingBuilder.bind(paySuccessQueue)
                .to(paySuccessExchange)
                .with(BookMallRabbitMq.PAY_SUCCESS_ROUTING_KEY);
    }
}

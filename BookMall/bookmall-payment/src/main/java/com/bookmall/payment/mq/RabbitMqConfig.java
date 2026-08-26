package com.bookmall.payment.mq;

import com.bookmall.common.mq.BookMallRabbitMq;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付服务声明支付成功交换机、队列和绑定。
 * 订单服务也会重复声明同名资源，RabbitMQ 声明本身是幂等的，可避免启动顺序问题。
 */
@Configuration
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

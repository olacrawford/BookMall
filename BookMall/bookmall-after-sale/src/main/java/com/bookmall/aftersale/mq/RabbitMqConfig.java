package com.bookmall.aftersale.mq;

import com.bookmall.common.mq.BookMallRabbitMq;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Bean
    public TopicExchange afterSaleEventExchange() {
        return new TopicExchange(BookMallRabbitMq.AFTER_SALE_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue afterSaleRefundQueue() {
        return new Queue(BookMallRabbitMq.AFTER_SALE_REFUND_QUEUE, true);
    }

    @Bean
    public Binding afterSaleRefundBinding(TopicExchange afterSaleEventExchange, Queue afterSaleRefundQueue) {
        return BindingBuilder.bind(afterSaleRefundQueue)
                .to(afterSaleEventExchange)
                .with(BookMallRabbitMq.AFTER_SALE_REFUND_ROUTING_KEY);
    }
}

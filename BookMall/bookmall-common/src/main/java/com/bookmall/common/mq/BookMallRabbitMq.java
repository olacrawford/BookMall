package com.bookmall.common.mq;

/**
 * 支付成功事件的 RabbitMQ 交换机、路由和队列常量。
 */
public final class BookMallRabbitMq {

    private BookMallRabbitMq() {
    }

    public static final String PAY_SUCCESS_EXCHANGE = "bookmall.pay.success.exchange";
    public static final String PAY_SUCCESS_QUEUE = "bookmall.order.pay.success.queue";
    public static final String PAY_SUCCESS_ROUTING_KEY = "pay.success";
}

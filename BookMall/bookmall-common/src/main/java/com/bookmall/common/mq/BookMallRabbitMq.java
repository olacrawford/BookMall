package com.bookmall.common.mq;

/**
 * BookMall RabbitMQ 消息拓扑常量。
 */
public final class BookMallRabbitMq {

    private BookMallRabbitMq() {
    }

    // 支付成功事件：payment -> order
    public static final String PAY_SUCCESS_EXCHANGE = "bookmall.pay.success.exchange";
    public static final String PAY_SUCCESS_QUEUE = "bookmall.order.pay.success.queue";
    public static final String PAY_SUCCESS_ROUTING_KEY = "pay.success";

    // 订单状态事件：order -> stock
    public static final String ORDER_STOCK_EXCHANGE = "bookmall.order.stock.exchange";
    public static final String ORDER_PAID_QUEUE = "bookmall.stock.order.paid.queue";
    public static final String ORDER_STOCK_RELEASE_QUEUE = "bookmall.stock.order.release.queue";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";
    public static final String ORDER_STOCK_RELEASE_ROUTING_KEY = "order.stock.release";
}

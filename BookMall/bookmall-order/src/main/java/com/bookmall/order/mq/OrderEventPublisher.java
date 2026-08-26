package com.bookmall.order.mq;

import com.bookmall.common.mq.BookMallRabbitMq;
import com.bookmall.common.mq.OrderStockEvent;
import com.bookmall.common.mq.StockItemMessage;
import com.bookmall.order.client.dto.StockOperationItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发布订单支付/释放库存事件，库存服务订阅后异步处理。
 */
@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderPaid(Long orderId, Long userId, List<StockOperationItem> stockItems) throws Exception {
        publish(OrderStockEvent.OPERATION_ORDER_PAID, orderId, userId, stockItems,
                BookMallRabbitMq.ORDER_PAID_ROUTING_KEY);
    }

    public void publishStockRelease(Long orderId, Long userId, List<StockOperationItem> stockItems) throws Exception {
        publish(OrderStockEvent.OPERATION_ORDER_RELEASE, orderId, userId, stockItems,
                BookMallRabbitMq.ORDER_STOCK_RELEASE_ROUTING_KEY);
    }

    private void publish(String operation, Long orderId, Long userId,
                         List<StockOperationItem> stockItems, String routingKey) throws Exception {
        OrderStockEvent event = new OrderStockEvent();
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setOperation(operation);
        event.setItems(stockItems.stream()
                .map(item -> new StockItemMessage(item.getBookId(), item.getQuantity()))
                .toList());
        String payload = objectMapper.writeValueAsString(event);
        rabbitTemplate.convertAndSend(BookMallRabbitMq.ORDER_STOCK_EXCHANGE, routingKey, payload);
    }
}

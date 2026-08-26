package com.bookmall.stock.mq;

import com.bookmall.common.mq.BookMallRabbitMq;
import com.bookmall.common.mq.OrderStockEvent;
import com.bookmall.stock.dto.StockOperationItem;
import com.bookmall.stock.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消费订单服务发布的库存确认/释放事件。
 */
@Slf4j
@Component
public class OrderStockConsumer {

    private final ObjectMapper objectMapper;
    private final StockService stockService;

    public OrderStockConsumer(ObjectMapper objectMapper, StockService stockService) {
        this.objectMapper = objectMapper;
        this.stockService = stockService;
    }

    @RabbitListener(queues = {BookMallRabbitMq.ORDER_PAID_QUEUE, BookMallRabbitMq.ORDER_STOCK_RELEASE_QUEUE})
    public void onOrderStockEvent(String payload) throws Exception {
        OrderStockEvent event = objectMapper.readValue(payload, OrderStockEvent.class);
        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("订单库存事件缺少需要确认或释放的商品明细：{}", payload);
            return;
        }
        if (OrderStockEvent.OPERATION_ORDER_PAID.equals(event.getOperation())
                && event.getOrderId() == null) {
            log.warn("订单支付库存确认事件缺少订单号：{}", payload);
            return;
        }

        List<StockOperationItem> items = event.getItems().stream()
                .map(item -> new StockOperationItem(item.getBookId(), item.getQuantity()))
                .toList();
        if (OrderStockEvent.OPERATION_ORDER_PAID.equals(event.getOperation())) {
            stockService.confirm(items);
            log.info("订单支付库存确认完成：orderId={}, eventId={}", event.getOrderId(), event.getEventId());
        } else {
            stockService.release(items);
            log.info("订单库存释放完成：orderId={}, eventId={}", event.getOrderId(), event.getEventId());
        }
    }
}

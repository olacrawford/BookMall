package com.bookmall.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.order.entity.Order;
import com.bookmall.order.mapper.OrderMapper;
import com.bookmall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时任务：周期性扫描超时未支付订单并自动取消。
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public OrderTimeoutTask(OrderMapper orderMapper, OrderService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Scheduled(cron = "${bookmall.order.close-cron:0/30 * * * * ?}")
    public void closeExpiredOrders() {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)
                .isNotNull(Order::getExpireTime)
                .le(Order::getExpireTime, LocalDateTime.now())
                .orderByAsc(Order::getExpireTime));

        for (Order order : orders) {
            try {
                if (orderService.closeExpiredOrder(order.getId())) {
                    log.info("自动关闭超时订单：orderId={}", order.getId());
                }
            } catch (RuntimeException ex) {
                // 保留库存状态和订单状态，等待下一轮重试
                log.warn("自动关闭超时订单失败：orderId={}", order.getId(), ex);
            }
        }
    }
}

package com.bookmall.order.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.bookmall.order.client.BookClient;
import com.bookmall.order.client.CartClient;
import com.bookmall.order.client.StockClient;
import com.bookmall.order.entity.Order;
import com.bookmall.order.entity.OrderItem;
import com.bookmall.order.mapper.OrderItemMapper;
import com.bookmall.order.mapper.OrderMapper;
import com.bookmall.order.mq.OrderEventPublisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private BookClient bookClient;
    @Mock
    private CartClient cartClient;
    @Mock
    private StockClient stockClient;
    @Mock
    private OrderEventPublisher orderEventPublisher;

    private OrderServiceImpl orderService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
    }

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderMapper, orderItemMapper, bookClient, cartClient,
                stockClient, orderEventPublisher, 30);
    }

    @Test
    void completeOrder_updatesPaidOrder_whenOwnedByUser() {
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        assertTrue(orderService.completeOrder(100L, 1L));

        verify(orderMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void completeOrder_returnsTrue_whenAlreadyCompleted() {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(3);

        when(orderMapper.update(isNull(), any())).thenReturn(0);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertTrue(orderService.completeOrder(100L, 1L));
        verify(orderMapper).selectById(100L);
    }

    @Test
    void completeOrder_returnsFalse_whenOrderIsStillPending() {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(0);

        when(orderMapper.update(isNull(), any())).thenReturn(0);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertFalse(orderService.completeOrder(100L, 1L));
    }

    @Test
    void completeOrder_returnsFalse_whenOrderBelongsToAnotherUser() {
        Order order = new Order();
        order.setUserId(2L);
        order.setStatus(3);

        when(orderMapper.update(isNull(), any())).thenReturn(0);
        when(orderMapper.selectById(100L)).thenReturn(order);

        assertFalse(orderService.completeOrder(100L, 1L));
    }

    @Test
    void markPaid_publishesPaidEventOnce_whenAlreadyPaid() throws Exception {
        Order existing = new Order();
        existing.setId(100L);
        existing.setUserId(1L);
        existing.setStatus(1);

        OrderItem item = new OrderItem();
        item.setBookId(5L);
        item.setQuantity(2);

        when(orderMapper.update(isNull(), any())).thenReturn(0);
        when(orderMapper.selectById(100L)).thenReturn(existing);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        assertTrue(orderService.markPaid(100L, 1L));

        verify(orderEventPublisher).publishOrderPaid(eq(100L), eq(1L), anyList());
    }

    @Test
    void cancelOrder_publishesReleaseEvent_whenCancelled() throws Exception {
        OrderItem item = new OrderItem();
        item.setBookId(5L);
        item.setQuantity(2);

        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        assertTrue(orderService.cancelOrder(100L, 1L));

        verify(orderEventPublisher).publishStockRelease(eq(100L), eq(1L), anyList());
    }

    @Test
    void closeExpiredOrder_publishesReleaseEvent_whenClosed() throws Exception {
        OrderItem item = new OrderItem();
        item.setBookId(5L);
        item.setQuantity(2);

        when(orderMapper.update(isNull(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        assertTrue(orderService.closeExpiredOrder(100L));

        verify(orderEventPublisher).publishStockRelease(eq(100L), isNull(), anyList());
    }
}
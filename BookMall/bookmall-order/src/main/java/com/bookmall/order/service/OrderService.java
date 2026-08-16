package com.bookmall.order.service;

import com.bookmall.order.dto.OrderCreateRequest;
import com.bookmall.order.vo.OrderDetailVO;
import com.bookmall.order.vo.OrderVO;

import java.util.List;

public interface OrderService {

    OrderDetailVO createOrder(OrderCreateRequest request);

    List<OrderVO> listOrdersByUserId(Long userId);

    OrderDetailVO getOrderDetail(Long id);

    boolean cancelOrder(Long id);
}
package com.bookmall.order.controller;

import com.bookmall.common.result.Result;
import com.bookmall.order.dto.OrderCreateRequest;
import com.bookmall.order.service.OrderService;
import com.bookmall.order.vo.OrderDetailVO;
import com.bookmall.order.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 健康检查
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("bookmall-order is running");
    }

    // 下单：userId 由网关鉴权后透传，不从请求体取
    @PostMapping
    public Result<OrderDetailVO> createOrder(@RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody OrderCreateRequest request) {
        OrderDetailVO detail = orderService.createOrder(userId, request);
        if (detail == null) {
            return Result.fail(400, "下单失败，请检查图书信息");
        }
        return Result.success(detail);
    }

    // 查询当前用户的订单列表
    @GetMapping
    public Result<List<OrderVO>> listOrders(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.listOrdersByUserId(userId));
    }

    // 查询订单详情
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@RequestHeader("X-User-Id") Long userId,
                                                @PathVariable("id") Long id) {
        OrderDetailVO detail = orderService.getOrderDetail(id, userId);
        if (detail == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(detail);
    }

    // 取消订单
    @PutMapping("/{id}/cancel")
    public Result<String> cancelOrder(@RequestHeader("X-User-Id") Long userId,
                                      @PathVariable("id") Long id) {
        boolean cancelled = orderService.cancelOrder(id, userId);
        if (!cancelled) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success("取消成功");
    }
}

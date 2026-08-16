package com.bookmall.order.controller;

import com.bookmall.common.result.Result;
import com.bookmall.order.dto.OrderCreateRequest;
import com.bookmall.order.service.OrderService;
import com.bookmall.order.vo.OrderDetailVO;
import com.bookmall.order.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("bookmall-order is running");
    }

    @PostMapping
    public Result<OrderDetailVO> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        OrderDetailVO detail = orderService.createOrder(request);
        if (detail == null) {
            return Result.fail(400, "下单失败，请检查地址、购物车或库存");
        }
        return Result.success(detail);
    }

    @GetMapping
    public Result<List<OrderVO>> listOrders(@RequestParam("userId") Long userId) {
        return Result.success(orderService.listOrdersByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable("id") Long id) {
        OrderDetailVO detail = orderService.getOrderDetail(id);
        if (detail == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(detail);
    }

    @PutMapping("/{id}/cancel")
    public Result<String> cancelOrder(@PathVariable("id") Long id) {
        boolean cancelled = orderService.cancelOrder(id);
        if (!cancelled) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success("取消成功");
    }
}
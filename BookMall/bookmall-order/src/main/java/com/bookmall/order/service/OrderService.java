package com.bookmall.order.service;

import com.bookmall.order.dto.OrderCreateRequest;
import com.bookmall.order.vo.OrderDetailVO;
import com.bookmall.order.vo.OrderVO;

import java.util.List;

/**
 * 订单业务接口
 */
public interface OrderService {

    /**
     * 直接下单：根据图书id和数量创建订单
     * @param userId 当前登录用户id（由网关鉴权后透传）
     * @param request 下单请求DTO
     * @return 创建完成的订单详情，图书不存在返回null
     */
    OrderDetailVO createOrder(Long userId, OrderCreateRequest request);

    /**
     * 查询某用户的订单列表
     * @param userId 用户id
     * @return 订单VO集合
     */
    List<OrderVO> listOrdersByUserId(Long userId);

    /**
     * 查询订单详情（只能查自己的订单）
     * @param id 订单id
     * @param userId 当前用户id
     * @return 订单详情VO，订单不存在或不属于该用户返回null
     */
    OrderDetailVO getOrderDetail(Long id, Long userId);

    /**
     * 取消订单（只能取消自己的待支付订单）
     * @param id 订单id
     * @param userId 当前用户id
     * @return true取消成功；false订单不存在/不属于该用户/状态不允许
     */
    boolean cancelOrder(Long id, Long userId);
}

package com.bookmall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.common.result.Result;
import com.bookmall.order.client.BookClient;
import com.bookmall.order.client.dto.BookSnapshot;
import com.bookmall.order.dto.OrderCreateRequest;
import com.bookmall.order.entity.Order;
import com.bookmall.order.entity.OrderItem;
import com.bookmall.order.mapper.OrderItemMapper;
import com.bookmall.order.mapper.OrderMapper;
import com.bookmall.order.service.OrderService;
import com.bookmall.order.vo.OrderDetailVO;
import com.bookmall.order.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单业务实现
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final BookClient bookClient;

    // 构造注入 mapper 和 Feign 客户端
    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            BookClient bookClient) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.bookClient = bookClient;
    }

    /**
     * 直接下单：Feign 调图书服务拿价格/标题 → 算总价 → 落订单 + 明细
     * @param userId 当前登录用户id
     * @param request 下单请求
     * @return 订单详情，图书不存在返回null
     */
    @Override
    //事务注解
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO createOrder(Long userId, OrderCreateRequest request) {
        // 通过 Feign 调图书服务拿价格与标题（下单即快照，避免后续改价影响历史订单）
        BookSnapshot book = successfulData(bookClient.getBookById(request.getBookId()));
        if (book == null || book.getPrice() == null) {
            return null;
        }

        // 总价 = 单价 × 数量
        BigDecimal totalAmount = book.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // 订单号：OD + 时间戳 + UUID 前6位，保证唯一
        String orderNo = "OD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 0 待支付
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 订单明细：快照图书标题和单价
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setBookId(book.getId());
        orderItem.setBookTitle(book.getTitle());
        orderItem.setBookPrice(book.getPrice());
        orderItem.setQuantity(request.getQuantity());
        orderItem.setSubtotal(totalAmount);
        orderItem.setCreateTime(LocalDateTime.now());
        orderItemMapper.insert(orderItem);

        return getOrderDetail(order.getId(), userId);
    }

    // 判断远程调用是否成功（code == 200）
    private boolean isSuccess(Result<?> result) {
        return result != null && Integer.valueOf(200).equals(result.getCode());
    }

    // 取出远程调用成功时的 data，失败返回 null
    private <T> T successfulData(Result<T> result) {
        return isSuccess(result) ? result.getData() : null;
    }

    /**
     * 查询某用户的订单列表（按创建时间倒序）
     */
    @Override
    public List<OrderVO> listOrdersByUserId(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
        );

        return orders.stream()
                .map(order -> {
                    OrderVO vo = new OrderVO();
                    vo.setId(order.getId());
                    vo.setOrderNo(order.getOrderNo());
                    vo.setUserId(order.getUserId());
                    vo.setTotalAmount(order.getTotalAmount());
                    vo.setStatus(order.getStatus());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询订单详情（只能查自己的订单）
     */
    @Override
    public OrderDetailVO getOrderDetail(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        // 只能查看自己的订单
        if (order == null || !order.getUserId().equals(userId)) {
            return null;
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, id)
        );

        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());

        // 订单明细转 VO 列表
        List<OrderDetailVO.OrderItemVO> itemVOList = items.stream()
                .map(item -> {
                    OrderDetailVO.OrderItemVO itemVO = new OrderDetailVO.OrderItemVO();
                    itemVO.setBookId(item.getBookId());
                    itemVO.setBookTitle(item.getBookTitle());
                    itemVO.setBookPrice(item.getBookPrice());
                    itemVO.setQuantity(item.getQuantity());
                    itemVO.setSubtotal(item.getSubtotal());
                    return itemVO;
                })
                .collect(Collectors.toList());

        vo.setItems(itemVOList);
        return vo;
    }

    /**
     * 取消订单（只能取消自己的待支付订单）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long id, Long userId) {
        Order order = orderMapper.selectById(id);
        // 只能操作自己的订单
        if (order == null || !order.getUserId().equals(userId)) {
            return false;
        }
        // 仅待支付状态(0)的订单可取消
        if (order.getStatus() == null || order.getStatus() != 0) {
            return false;
        }

        order.setStatus(2); // 2 已取消
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return true;
    }
}

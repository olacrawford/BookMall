package com.bookmall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.cart.entity.Cart;
import com.bookmall.common.result.Result;
import com.bookmall.order.client.AddressClient;
import com.bookmall.order.client.BookClient;
import com.bookmall.order.client.InventoryClient;
import com.bookmall.order.client.dto.AddressSnapshot;
import com.bookmall.order.client.dto.BookSnapshot;
import com.bookmall.order.dto.InventoryDeductRequest;
import com.bookmall.order.dto.InventoryRecoverRequest;
import com.bookmall.order.dto.OrderCreateRequest;
import com.bookmall.order.entity.Order;
import com.bookmall.order.entity.OrderItem;
import com.bookmall.order.mapper.CartMapper;
import com.bookmall.order.mapper.OrderItemMapper;
import com.bookmall.order.mapper.OrderMapper;
import com.bookmall.order.service.OrderService;
import com.bookmall.order.vo.OrderDetailVO;
import com.bookmall.order.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final InventoryClient inventoryClient;
    private final BookClient bookClient;
    private final AddressClient addressClient;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            CartMapper cartMapper,
                            InventoryClient inventoryClient,
                            BookClient bookClient,
                            AddressClient addressClient) {

        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.inventoryClient = inventoryClient;
        this.bookClient = bookClient;
        this.addressClient = addressClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO createOrder(OrderCreateRequest request) {
        List<Cart> cartItems = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, request.getUserId())
                        .in(Cart::getId, request.getCartItemIds())
        );

        if (cartItems.isEmpty() || cartItems.size() != request.getCartItemIds().size()) {
            return null;
        }

        Result<AddressSnapshot> addressResult = addressClient.getAddressById(request.getAddressId());
        AddressSnapshot address = successfulData(addressResult);
        if (address == null) {
            return null;
        }
        if (address.getUserId() != null && !address.getUserId().equals(request.getUserId())) {
            return null;
        }

        List<Cart> deductedItems = new ArrayList<>();
        List<BookSnapshot> bookList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            for (Cart cart : cartItems) {
                InventoryDeductRequest deductRequest = new InventoryDeductRequest();
                deductRequest.setBookId(cart.getBookId());
                deductRequest.setQuantity(cart.getQuantity());

                if (!isSuccess(inventoryClient.deduct(deductRequest))) {
                    rollbackInventory(deductedItems);
                    return null;
                }
                deductedItems.add(cart);

                BookSnapshot book = successfulData(bookClient.getBookById(cart.getBookId()));
                if (book == null || book.getPrice() == null) {
                    rollbackInventory(deductedItems);
                    return null;
                }

                bookList.add(book);
                BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
                totalAmount = totalAmount.add(subtotal);
            }

            String orderNo = "OD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);

            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId());
            order.setTotalAmount(totalAmount);
            order.setStatus(0);
            order.setPayStatus(0);
            // 收货信息快照直接落订单，避免后续地址变更影响历史订单。
            order.setReceiverName(address.getReceiverName());
            order.setReceiverPhone(address.getReceiverPhone());
            order.setReceiverAddress(address.getFullAddress());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.insert(order);

            for (int i = 0; i < cartItems.size(); i++) {
                Cart cart = cartItems.get(i);
                BookSnapshot book = bookList.get(i);
                BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                // 图书标题和价格写入明细快照，保证历史订单可追溯。
                orderItem.setBookId(cart.getBookId());
                orderItem.setBookTitle(book.getTitle());
                orderItem.setBookPrice(book.getPrice());
                orderItem.setQuantity(cart.getQuantity());
                orderItem.setSubtotal(subtotal);
                orderItem.setCreateTime(LocalDateTime.now());
                orderItemMapper.insert(orderItem);
            }

            cartMapper.deleteByIds(cartItems.stream().map(Cart::getId).toList());
            return getOrderDetail(order.getId());
        } catch (Exception e) {
            // 下单链路任一步失败，都尽力把已扣减库存补偿回去。
            rollbackInventory(deductedItems);
            throw e;
        }
    }

    private void rollbackInventory(List<Cart> deductedItems) {
        for (Cart cart : deductedItems) {
            InventoryRecoverRequest recoverRequest = new InventoryRecoverRequest();
            recoverRequest.setBookId(cart.getBookId());
            recoverRequest.setQuantity(cart.getQuantity());
            inventoryClient.recover(recoverRequest);
        }
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && Integer.valueOf(200).equals(result.getCode());
    }

    private <T> T successfulData(Result<T> result) {
        return isSuccess(result) ? result.getData() : null;
    }

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
                    vo.setPayStatus(order.getPayStatus());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
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
        vo.setPayStatus(order.getPayStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return false;
        }

        if (order.getStatus() == null || order.getStatus() != 0) {
            return false;
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, id)
        );

        for (OrderItem item : items) {
            InventoryRecoverRequest recoverRequest = new InventoryRecoverRequest();
            recoverRequest.setBookId(item.getBookId());
            recoverRequest.setQuantity(item.getQuantity());

            if (!isSuccess(inventoryClient.recover(recoverRequest))) {
                return false;
            }
        }

        order.setStatus(2);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return true;
    }
}

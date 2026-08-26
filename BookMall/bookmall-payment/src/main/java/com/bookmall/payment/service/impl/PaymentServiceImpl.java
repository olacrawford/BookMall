package com.bookmall.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.common.exception.BusinessException;
import com.bookmall.common.result.Result;
import com.bookmall.payment.client.OrderClient;
import com.bookmall.payment.client.dto.OrderSnapshot;
import com.bookmall.payment.dto.PaymentRequest;
import com.bookmall.payment.entity.Payment;
import com.bookmall.payment.mapper.PaymentMapper;
import com.bookmall.payment.service.PaymentService;
import com.bookmall.payment.vo.PaymentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付业务：当前使用内部模拟支付，先落支付单，再把订单更新为已支付。
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderClient orderClient;

    public PaymentServiceImpl(PaymentMapper paymentMapper, OrderClient orderClient) {
        this.paymentMapper = paymentMapper;
        this.orderClient = orderClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO pay(Long userId, PaymentRequest request) {
        // 先通过订单服务校验订单归属，金额以订单服务返回为准
        OrderSnapshot order = getPayableOrder(userId, request.getOrderId());
        Payment payment = findByOrderId(userId, request.getOrderId());

        // 幂等处理：已存在支付单且已支付时直接返回，避免重复扣款
        if (payment != null && Integer.valueOf(1).equals(payment.getStatus())) {
            markOrderPaid(order.getId(), userId);
            return toVO(payment);
        }

        // 没有支付单就新建；已有支付单但未成功时更新为已支付
        if (payment == null) {
            payment = createPayment(order, userId);
        } else {
            payment.setStatus(1);
            payment.setPayTime(LocalDateTime.now());
            payment.setUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);
        }

        // 支付单落库成功后再同步订单状态，失败时本地事务整体回滚
        markOrderPaid(order.getId(), userId);
        return toVO(payment);
    }

    @Override
    public PaymentVO getByOrderId(Long userId, Long orderId) {
        // 只允许当前用户查询自己的支付单
        Payment payment = findByOrderId(userId, orderId);
        if (payment == null) {
            throw new BusinessException(404, "支付单不存在");
        }
        return toVO(payment);
    }

    private OrderSnapshot getPayableOrder(Long userId, Long orderId) {
        // 远程调用订单服务，避免支付服务直接信任前端传入的订单信息
        Result<OrderSnapshot> result = orderClient.getOrderDetail(orderId, userId);
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(404, "订单不存在");
        }
        OrderSnapshot order = result.getData();
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限支付该订单");
        }
        if (order.getStatus() != null && (order.getStatus() == 2 || order.getStatus() == 3)) {
            throw new BusinessException(400, "订单当前状态不可支付");
        }
        // 即使超时任务还没执行，也不允许继续支付已过期订单
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(400, "订单已超时，请重新下单");
        }
        return order;
    }

    private Payment findByOrderId(Long userId, Long orderId) {
        // 一个订单只保留一条支付记录，用于幂等判断
        return paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getUserId, userId));
    }

    private Payment createPayment(OrderSnapshot order, Long userId) {
        // 当前为内部模拟支付，落单后直接标记为已支付
        String paymentNo = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(userId);
        payment.setAmount(order.getTotalAmount());
        payment.setPayType("mock");
        payment.setStatus(1);
        payment.setPayTime(now);
        payment.setCreateTime(now);
        payment.setUpdateTime(now);
        paymentMapper.insert(payment);
        return payment;
    }

    private void markOrderPaid(Long orderId, Long userId) {
        // Feign 调用订单服务更新支付状态，非 200 时抛出异常触发本地回滚
        Result<Void> result = orderClient.markPaid(orderId, userId);
        if (result == null || !Integer.valueOf(200).equals(result.getCode())) {
            String message = result != null && result.getMessage() != null ? result.getMessage() : "订单支付状态更新失败";
            throw new BusinessException(500, message);
        }
    }

    private PaymentVO toVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        vo.setId(payment.getId());
        vo.setPaymentNo(payment.getPaymentNo());
        vo.setOrderId(payment.getOrderId());
        vo.setOrderNo(payment.getOrderNo());
        vo.setAmount(payment.getAmount());
        vo.setPayType(payment.getPayType());
        vo.setStatus(payment.getStatus());
        vo.setPayTime(payment.getPayTime());
        return vo;
    }
}

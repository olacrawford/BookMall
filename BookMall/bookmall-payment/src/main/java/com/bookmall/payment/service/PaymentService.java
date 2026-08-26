package com.bookmall.payment.service;

import com.bookmall.payment.dto.PaymentRequest;
import com.bookmall.payment.vo.PaymentVO;

public interface PaymentService {

    PaymentVO pay(Long userId, PaymentRequest request);

    PaymentVO getByOrderId(Long userId, Long orderId);
}

package com.bookmall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;

}
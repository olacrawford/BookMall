package com.bookmall.order.client.dto;

import lombok.Data;

@Data
public class CartItemSnapshot {

    private Long id;
    private Long bookId;
    private Integer quantity;
    private Integer selected;
}

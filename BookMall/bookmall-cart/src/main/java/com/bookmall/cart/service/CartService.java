package com.bookmall.cart.service;

import com.bookmall.cart.dto.CartAddRequest;
import com.bookmall.cart.entity.Cart;

import java.util.List;

public interface CartService {

    List<Cart> listCartByUserId(Long userId);

    Cart addToCart(CartAddRequest request);

    Cart updateQuantity(Long id, Integer quantity);

    boolean deleteItem(Long id);
}
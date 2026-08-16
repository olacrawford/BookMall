package com.bookmall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.cart.dto.CartAddRequest;
import com.bookmall.cart.entity.Cart;
import com.bookmall.cart.mapper.CartMapper;
import com.bookmall.cart.service.CartService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;

    public CartServiceImpl(CartMapper cartMapper) {
        this.cartMapper = cartMapper;
    }

    @Override
    public List<Cart> listCartByUserId(Long userId) {
        return cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getUpdateTime)
        );
    }

    @Override
    public Cart addToCart(CartAddRequest request) {
        Cart exist = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, request.getUserId())
                        .eq(Cart::getBookId, request.getBookId())
        );

        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + request.getQuantity());
            cartMapper.updateById(exist);
            return exist;
        }

        Cart cart = new Cart();
        cart.setUserId(request.getUserId());
        cart.setBookId(request.getBookId());
        cart.setQuantity(request.getQuantity());
        cart.setSelected(1);
        cart.setCreateTime(java.time.LocalDateTime.now());
        cart.setUpdateTime(java.time.LocalDateTime.now());

        cartMapper.insert(cart);
        return cart;
    }

    @Override
    public Cart updateQuantity(Long id, Integer quantity) {
        Cart cart = cartMapper.selectById(id);
        if (cart == null) {
            return null;
        }

        cart.setQuantity(quantity);
        cart.setUpdateTime(java.time.LocalDateTime.now());
        cartMapper.updateById(cart);
        return cart;
    }

    @Override
    public boolean deleteItem(Long id) {
        Cart cart = cartMapper.selectById(id);
        if (cart == null) {
            return false;
        }

        cartMapper.deleteById(id);
        return true;
    }

}
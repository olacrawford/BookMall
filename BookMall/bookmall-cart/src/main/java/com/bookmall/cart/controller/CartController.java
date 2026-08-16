package com.bookmall.cart.controller;

import com.bookmall.cart.dto.CartAddRequest;
import com.bookmall.cart.dto.CartUpdateRequest;
import com.bookmall.cart.entity.Cart;
import com.bookmall.cart.service.CartService;
import com.bookmall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("bookmall-cart is running");
    }

    @GetMapping
    public Result<List<Cart>> listCart(@RequestParam("userId") Long userId) {
        return Result.success(cartService.listCartByUserId(userId));
    }

    @PostMapping("/items")
    public Result<Cart> addToCart(@Valid @RequestBody CartAddRequest request) {
        return Result.success(cartService.addToCart(request));
    }

    @PutMapping("/items/{id}")
    public Result<Cart> updateQuantity(@PathVariable("id") Long id,
                                       @Valid @RequestBody CartUpdateRequest request) {
        Cart cart = cartService.updateQuantity(id, request.getQuantity());
        if (cart == null) {
            return Result.fail(404, "购物车项不存在");
        }
        return Result.success(cart);
    }

    @DeleteMapping("/items/{id}")
    public Result<String> deleteItem(@PathVariable("id") Long id) {
        boolean deleted = cartService.deleteItem(id);
        if (!deleted) {
            return Result.fail(404, "购物车项不存在");
        }
        return Result.success("删除成功");
    }
}
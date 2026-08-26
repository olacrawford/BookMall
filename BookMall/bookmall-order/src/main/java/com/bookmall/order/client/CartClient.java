package com.bookmall.order.client;

import com.bookmall.common.result.Result;
import com.bookmall.order.client.dto.CartItemSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "cart")
public interface CartClient {

    // 订单服务通过 Nacos 服务名读取当前用户已勾选的购物车条目
    @GetMapping("/cart/selected")
    Result<List<CartItemSnapshot>> selectedItems(@RequestHeader("X-User-Id") Long userId);
}

package com.bookmall.aftersale.client;

import com.bookmall.aftersale.client.dto.OrderSnapshot;
import com.bookmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order")
public interface OrderClient {

    @GetMapping("/orders/{id}")
    Result<OrderSnapshot> getOrderDetail(@PathVariable("id") Long id,
                                         @RequestHeader("X-User-Id") Long userId);
}

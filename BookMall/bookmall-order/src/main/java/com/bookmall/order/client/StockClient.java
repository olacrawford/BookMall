package com.bookmall.order.client;

import com.bookmall.common.result.Result;
import com.bookmall.order.client.dto.StockOperationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stock")
public interface StockClient {

    // 下单前预占库存
    @PostMapping("/stock/deduct")
    Result<Void> deduct(@RequestBody StockOperationRequest request);

}

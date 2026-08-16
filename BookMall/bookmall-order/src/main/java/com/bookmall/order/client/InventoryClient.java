package com.bookmall.order.client;

import com.bookmall.common.result.Result;
import com.bookmall.order.dto.InventoryDeductRequest;
import com.bookmall.order.dto.InventoryRecoverRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory")
public interface InventoryClient {

    @PostMapping("/inventory/deduct")
    Result<String> deduct(@RequestBody InventoryDeductRequest request);

    @PostMapping("/inventory/recover")
    Result<String> recover(@RequestBody InventoryRecoverRequest request);
}

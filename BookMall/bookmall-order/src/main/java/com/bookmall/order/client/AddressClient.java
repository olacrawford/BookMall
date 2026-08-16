package com.bookmall.order.client;

import com.bookmall.common.result.Result;
import com.bookmall.order.client.dto.AddressSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "address")
public interface AddressClient {

    @GetMapping("/address/{id}")
    Result<AddressSnapshot> getAddressById(@PathVariable("id") Long addressId);
}

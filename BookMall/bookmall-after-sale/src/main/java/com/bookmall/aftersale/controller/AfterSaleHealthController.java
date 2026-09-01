package com.bookmall.aftersale.controller;

import com.bookmall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/after-sales")
public class AfterSaleHealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("bookmall-after-sale is running");
    }
}

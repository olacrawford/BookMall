package com.bookmall.aftersale.controller;

import com.bookmall.aftersale.dto.AfterSaleCreateRequest;
import com.bookmall.aftersale.dto.RefundRequest;
import com.bookmall.aftersale.service.AfterSaleService;
import com.bookmall.aftersale.vo.AfterSaleDetailVO;
import com.bookmall.aftersale.vo.AfterSaleVO;
import com.bookmall.aftersale.vo.RefundVO;
import com.bookmall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/after-sales")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping
    public Result<AfterSaleDetailVO> create(@RequestHeader("X-User-Id") Long userId,
                                            @Valid @RequestBody AfterSaleCreateRequest request) {
        return Result.success(afterSaleService.createAfterSale(userId, request));
    }

    @GetMapping
    public Result<List<AfterSaleVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(afterSaleService.listAfterSales(userId));
    }

    @GetMapping("/{id}")
    public Result<AfterSaleDetailVO> detail(@RequestHeader("X-User-Id") Long userId,
                                            @PathVariable Long id) {
        return Result.success(afterSaleService.getAfterSale(userId, id));
    }

    @PostMapping("/{id}/refund")
    public Result<RefundVO> refund(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody RefundRequest request) {
        return Result.success(afterSaleService.refund(userId, id, request));
    }
}

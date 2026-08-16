package com.bookmall.inventory.controller;

import com.bookmall.common.result.Result;
import com.bookmall.inventory.dto.InventoryDeductRequest;
import com.bookmall.inventory.dto.InventoryRecoverRequest;
import com.bookmall.inventory.entity.Inventory;
import com.bookmall.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("bookmall-inventory is running");
    }

    //根据图书 ID 查询库存
    @GetMapping("/{bookId}")
    public Result<Inventory> getInventory(@PathVariable("bookId") Long bookId) {
        Inventory inventory = inventoryService.getInventoryByBookId(bookId);
        if (inventory == null) {
            return Result.fail(404, "库存不存在");
        }
        return Result.success(inventory);
    }

    //扣减库存
    @PostMapping("/deduct")
    public Result<String> deduct(@Valid @RequestBody InventoryDeductRequest request) {
        boolean success = inventoryService.deduct(request);
        if (!success) {
            return Result.fail(400, "库存不足或库存不存在");
        }
        return Result.success("扣减成功");
    }

    //恢复库存
    //用户下单->扣减库存->后来取消订单->恢复库存
    @PostMapping("/recover")
    public Result<String> recover(@Valid @RequestBody InventoryRecoverRequest request) {
        boolean success = inventoryService.recover(request);
        if (!success) {
            return Result.fail(400, "库存不存在");
        }
        return Result.success("恢复成功");
    }
}
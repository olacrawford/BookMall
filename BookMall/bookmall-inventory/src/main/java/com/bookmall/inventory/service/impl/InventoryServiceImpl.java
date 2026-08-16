package com.bookmall.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.inventory.dto.InventoryDeductRequest;
import com.bookmall.inventory.dto.InventoryRecoverRequest;
import com.bookmall.inventory.entity.Inventory;
import com.bookmall.inventory.mapper.InventoryMapper;
import com.bookmall.inventory.service.InventoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    public InventoryServiceImpl(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public Inventory getInventoryByBookId(Long bookId) {
        return inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getBookId, bookId)
        );
    }

    @Override
    public boolean deduct(InventoryDeductRequest request) {
        Inventory inventory = getInventoryByBookId(request.getBookId());
        if (inventory == null) {
            return false;
        }

        if (inventory.getAvailableStock() < request.getQuantity()) {
            return false;
        }

        // 下单阶段先把库存从可用区转到锁定区，避免重复售卖。
        inventory.setAvailableStock(inventory.getAvailableStock() - request.getQuantity());
        inventory.setLockedStock(inventory.getLockedStock() + request.getQuantity());
        inventory.setUpdateTime(LocalDateTime.now());
        inventoryMapper.updateById(inventory);
        return true;
    }

    @Override
    public boolean recover(InventoryRecoverRequest request) {
        Inventory inventory = getInventoryByBookId(request.getBookId());
        if (inventory == null) {
            return false;
        }

        inventory.setAvailableStock(inventory.getAvailableStock() + request.getQuantity());
        inventory.setLockedStock(Math.max(0, inventory.getLockedStock() - request.getQuantity()));
        inventory.setUpdateTime(LocalDateTime.now());
        inventoryMapper.updateById(inventory);
        return true;
    }
}

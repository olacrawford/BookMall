package com.bookmall.inventory.service;

import com.bookmall.inventory.dto.InventoryDeductRequest;
import com.bookmall.inventory.dto.InventoryRecoverRequest;
import com.bookmall.inventory.entity.Inventory;

public interface InventoryService {

    Inventory getInventoryByBookId(Long bookId);

    boolean deduct(InventoryDeductRequest request);

    boolean recover(InventoryRecoverRequest request);
}
package com.bookmall.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookmall.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
}
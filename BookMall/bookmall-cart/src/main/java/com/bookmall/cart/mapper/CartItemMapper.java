package com.bookmall.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookmall.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
    // MyBatis-Plus BaseMapper 已提供常用 CRUD，当前无需自定义 SQL
}

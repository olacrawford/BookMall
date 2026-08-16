package com.bookmall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookmall.cart.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}
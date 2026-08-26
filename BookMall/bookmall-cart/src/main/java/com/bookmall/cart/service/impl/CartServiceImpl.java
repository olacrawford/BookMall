package com.bookmall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.cart.client.BookClient;
import com.bookmall.cart.client.dto.BookSnapshot;
import com.bookmall.cart.dto.CartItemCreateRequest;
import com.bookmall.cart.dto.CartItemUpdateRequest;
import com.bookmall.cart.entity.CartItem;
import com.bookmall.cart.mapper.CartItemMapper;
import com.bookmall.cart.service.CartService;
import com.bookmall.cart.vo.CartItemVO;
import com.bookmall.common.exception.BusinessException;
import com.bookmall.common.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    // mapper 负责持久化，BookClient 负责校验图书是否还存在并且是上架状态
    private final CartItemMapper cartItemMapper;
    private final BookClient bookClient;

    public CartServiceImpl(CartItemMapper cartItemMapper, BookClient bookClient) {
        this.cartItemMapper = cartItemMapper;
        this.bookClient = bookClient;
    }

    @Override
    // 只查询当前用户的购物车，按最近更新时间倒序展示
    public List<CartItemVO> list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getUpdateTime));
        return items.stream().map(this::toVO).toList();
    }

    @Override
    // 提供给订单服务使用的已选条目，只返回 selected=1 的数据
    public List<CartItemVO> selected(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSelected, 1)
                .orderByDesc(CartItem::getUpdateTime));
        return items.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 新增购物车条目需要“查重 + 更新/插入”，两个步骤必须同时成功或同时回滚
    public CartItemVO add(Long userId, CartItemCreateRequest request) {
        // 通过 Feign 调用图书服务，避免购物车服务直接信任前端传来的 bookId
        validateBook(request.getBookId());

        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getBookId, request.getBookId()));

        if (existing != null) {
            // 同一用户同一本书再次加入时累加数量，并沿用原有的勾选状态
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setSelected(resolveSelected(request.getSelected(), existing.getSelected()));
            cartItemMapper.updateById(existing);
            return toVO(existing);
        }

        // 第一次加入这本书时创建新条目，未传 selected 默认勾选
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setBookId(request.getBookId());
        item.setQuantity(request.getQuantity());
        item.setSelected(resolveSelected(request.getSelected(), 1));
        cartItemMapper.insert(item);
        return toVO(item);
    }

    @Override
    // 修改数量或勾选状态前，先确认该购物车条目属于当前用户
    public CartItemVO update(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        CartItem item = getOwnedItem(userId, cartItemId);
        item.setQuantity(request.getQuantity());
        if (request.getSelected() != null) {
            item.setSelected(request.getSelected() ? 1 : 0);
        }
        cartItemMapper.updateById(item);
        return toVO(item);
    }

    @Override
    // 删除当前用户自己的购物车条目
    public void delete(Long userId, Long cartItemId) {
        CartItem item = getOwnedItem(userId, cartItemId);
        cartItemMapper.deleteById(item.getId());
    }

    @Override
    // 清空操作只会删除当前用户的数据
    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
    }

    // 同时匹配 id 和 userId，确保用户只能操作自己的购物车数据
    private CartItem getOwnedItem(Long userId, Long cartItemId) {
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getId, cartItemId)
                .eq(CartItem::getUserId, userId));
        if (item == null) {
            throw new BusinessException(404, "购物车商品不存在");
        }
        return item;
    }

    // 远程调用图书服务时校验 Result.code，避免把远程失败误当成图书不存在
    private void validateBook(Long bookId) {
        Result<BookSnapshot> result = bookClient.getBookById(bookId);
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(400, "图书不存在或已下架");
        }
    }

    // 前端没传 selected 时，新条目默认勾选，已有条目保持原状态
    private int resolveSelected(Boolean requested, Integer current) {
        if (requested != null) {
            return requested ? 1 : 0;
        }
        return current == null ? 1 : current;
    }

    private CartItemVO toVO(CartItem item) {
        CartItemVO vo = new CartItemVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setBookId(item.getBookId());
        vo.setQuantity(item.getQuantity());
        vo.setSelected(item.getSelected());
        return vo;
    }
}

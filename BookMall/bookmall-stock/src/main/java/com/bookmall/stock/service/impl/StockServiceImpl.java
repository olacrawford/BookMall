package com.bookmall.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.common.exception.BusinessException;
import com.bookmall.stock.dto.StockOperationItem;
import com.bookmall.stock.entity.BookStock;
import com.bookmall.stock.mapper.StockMapper;
import com.bookmall.stock.service.StockService;
import com.bookmall.stock.vo.StockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库存业务：查询、预占和释放。
 */
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;

    public StockServiceImpl(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Override
    public StockVO getByBookId(Long bookId) {
        // 只查询当前图书一条库存记录，不存在直接返回 404
        BookStock row = stockMapper.selectOne(new LambdaQueryWrapper<BookStock>()
                .eq(BookStock::getBookId, bookId));
        if (row == null) {
            throw new BusinessException(404, "库存不存在");
        }
        return toVO(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(List<StockOperationItem> items) {
        for (StockOperationItem item : items) {
            // 原子更新失败说明库存不足，事务会回滚本次所有预占
            int affected = stockMapper.deductStock(item.getBookId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessException(400, "库存不足：图书ID " + item.getBookId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(List<StockOperationItem> items) {
        for (StockOperationItem item : items) {
            // 原子更新返回0说明没有锁定库存，按已释放处理，避免历史订单或重试补偿卡住
            int affected = stockMapper.releaseStock(item.getBookId(), item.getQuantity());
            if (affected == 0) {
                log.info("库存无需释放或已释放：图书ID {}", item.getBookId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(List<StockOperationItem> items) {
        for (StockOperationItem item : items) {
            // 支付完成才允许确认；锁定库存不足时抛出异常，让订单事务回滚
            int affected = stockMapper.confirmStock(item.getBookId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessException(500, "库存确认失败：图书ID " + item.getBookId());
            }
        }
    }

    private StockVO toVO(BookStock row) {
        StockVO vo = new StockVO();
        vo.setBookId(row.getBookId());
        vo.setStock(row.getStock());
        vo.setLockedStock(row.getLockedStock());
        // 预占时 stock 已同步减少，因此可售库存直接使用 stock
        vo.setAvailableStock(row.getStock());
        return vo;
    }
}

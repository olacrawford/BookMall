package com.bookmall.stock.service;

import com.bookmall.stock.dto.StockOperationItem;
import com.bookmall.stock.vo.StockVO;

import java.util.List;

public interface StockService {

    StockVO getByBookId(Long bookId);

    void deduct(List<StockOperationItem> items);

    void release(List<StockOperationItem> items);

    void confirm(List<StockOperationItem> items);
}

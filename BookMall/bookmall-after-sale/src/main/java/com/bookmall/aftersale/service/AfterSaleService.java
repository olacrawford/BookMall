package com.bookmall.aftersale.service;

import com.bookmall.aftersale.dto.AfterSaleCreateRequest;
import com.bookmall.aftersale.dto.RefundRequest;
import com.bookmall.aftersale.vo.AfterSaleDetailVO;
import com.bookmall.aftersale.vo.AfterSaleVO;
import com.bookmall.aftersale.vo.RefundVO;

import java.util.List;

public interface AfterSaleService {

    AfterSaleDetailVO createAfterSale(Long userId, AfterSaleCreateRequest request);

    AfterSaleDetailVO getAfterSale(Long userId, Long id);

    List<AfterSaleVO> listAfterSales(Long userId);

    RefundVO refund(Long userId, Long afterSaleId, RefundRequest request);
}

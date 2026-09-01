package com.bookmall.aftersale.client;

public interface LogisticsQueryGateway {

    LogisticsQueryResult query(Long afterSaleId, Long orderId);
}

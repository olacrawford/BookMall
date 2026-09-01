package com.bookmall.aftersale.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoLogisticsQueryGatewayTest {

    @Test
    void query_returnsTimeout_whenModeIsTimeout() {
        DemoLogisticsQueryGateway gateway = new DemoLogisticsQueryGateway("timeout");

        LogisticsQueryResult result = gateway.query(90001L, 10001L);

        assertFalse(result.available());
        assertTrue(result.timeout());
    }

    @Test
    void query_returnsAvailable_whenModeIsOk() {
        DemoLogisticsQueryGateway gateway = new DemoLogisticsQueryGateway("ok");

        LogisticsQueryResult result = gateway.query(90001L, 10001L);

        assertTrue(result.available());
        assertFalse(result.timeout());
    }
}

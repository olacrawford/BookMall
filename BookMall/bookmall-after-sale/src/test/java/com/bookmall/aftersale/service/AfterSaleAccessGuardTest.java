package com.bookmall.aftersale.service;

import com.bookmall.aftersale.entity.AfterSaleOrder;
import com.bookmall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AfterSaleAccessGuardTest {

    private final AfterSaleAccessGuard guard = new AfterSaleAccessGuard(Set.of(1L));
    private final AfterSaleAccessGuard roleGuard = new AfterSaleAccessGuard(Set.of(), Set.of("APPROVER", "RISK"));

    @Test
    void requireOwner_allowsOrderOwner() {
        AfterSaleOrder order = new AfterSaleOrder();
        order.setUserId(10L);
        assertDoesNotThrow(() -> guard.requireOwner(order, 10L));
    }

    @Test
    void requireOwner_throws403_whenAnotherUser() {
        AfterSaleOrder order = new AfterSaleOrder();
        order.setUserId(10L);
        BusinessException ex = assertThrows(BusinessException.class, () -> guard.requireOwner(order, 11L));
        assert ex.getCode() == 403;
    }

    @Test
    void requireApprover_allowsConfiguredOperator() {
        assertDoesNotThrow(() -> guard.requireApprover(1L));
    }

    @Test
    void requireApprover_throws403_whenNormalUser() {
        BusinessException ex = assertThrows(BusinessException.class, () -> guard.requireApprover(2L));
        assert ex.getCode() == 403;
    }

    @Test
    void requireApprover_allowsApproverRoleFromJwtHeader() {
        assertDoesNotThrow(() -> roleGuard.requireApprover(9L, "USER,APPROVER"));
    }

    @Test
    void requireApprover_throws403_whenRoleHeaderIsNormalUser() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> roleGuard.requireApprover(9L, "USER"));
        assert ex.getCode() == 403;
    }
}

package com.bookmall.aftersale.service;

import com.bookmall.common.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AfterSaleStatusMachineTest {

    private final AfterSaleStatusMachine statusMachine = new AfterSaleStatusMachine();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "CREATED, UNDER_REVIEW",
            "UNDER_REVIEW, WAITING_APPROVAL",
            "UNDER_REVIEW, RISK_REVIEW",
            "UNDER_REVIEW, AUTO_HANDLED",
            "UNDER_REVIEW, WAITING_HUMAN",
            "UNDER_REVIEW, REJECTED",
            "UNDER_REVIEW, CANCELED",
            "WAITING_APPROVAL, PROCESSING",
            "WAITING_APPROVAL, REJECTED",
            "WAITING_APPROVAL, CANCELED",
            "RISK_REVIEW, PROCESSING",
            "RISK_REVIEW, REJECTED",
            "RISK_REVIEW, CANCELED",
            "AUTO_HANDLED, PROCESSING",
            "PROCESSING, COMPLETED",
            "PROCESSING, FAILED",
            "PROCESSING, WAITING_HUMAN",
            "WAITING_HUMAN, PROCESSING",
            "FAILED, PROCESSING"
    })
    void transition_returnsTarget_whenValid(String current, String target) {
        assertEquals(target, statusMachine.transition(current, target));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "CREATED, COMPLETED",
            "CREATED, PROCESSING",
            "CREATED, WAITING_APPROVAL",
            "UNDER_REVIEW, COMPLETED",
            "WAITING_APPROVAL, AUTO_HANDLED",
            "WAITING_HUMAN, COMPLETED",
            "PROCESSING, WAITING_APPROVAL",
            "PROCESSING, REJECTED",
            "COMPLETED, PROCESSING",
            "REJECTED, PROCESSING",
            "CANCELED, COMPLETED",
            "CREATED, CREATED"
    })
    void transition_throwsBusinessException_whenInvalid(String current, String target) {
        assertThrows(BusinessException.class, () -> statusMachine.transition(current, target));
    }
}

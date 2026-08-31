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
            "UNDER_REVIEW, APPROVAL",
            "UNDER_REVIEW, AUTO_HANDLED",
            "UNDER_REVIEW, REJECTED",
            "UNDER_REVIEW, CANCELED",
            "APPROVAL, PROCESSING",
            "APPROVAL, REJECTED",
            "APPROVAL, CANCELED",
            "PROCESSING, COMPLETED"
    })
    void transition_returnsTarget_whenValid(String current, String target) {
        assertEquals(target, statusMachine.transition(current, target));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "CREATED, COMPLETED",
            "CREATED, PROCESSING",
            "CREATED, APPROVAL",
            "UNDER_REVIEW, COMPLETED",
            "APPROVAL, AUTO_HANDLED",
            "PROCESSING, APPROVAL",
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

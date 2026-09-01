package com.bookmall.aftersale.service;

import com.bookmall.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class AfterSaleStatusMachine {


    public static final String CREATED = "CREATED";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String WAITING_APPROVAL = "WAITING_APPROVAL";
    public static final String RISK_REVIEW = "RISK_REVIEW";
    public static final String AUTO_HANDLED = "AUTO_HANDLED";
    public static final String WAITING_HUMAN = "WAITING_HUMAN";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELED = "CANCELED";
    public static final String FAILED = "FAILED";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            CREATED, Set.of(UNDER_REVIEW),
            UNDER_REVIEW, Set.of(WAITING_APPROVAL, RISK_REVIEW, AUTO_HANDLED, WAITING_HUMAN, REJECTED, CANCELED),
            WAITING_APPROVAL, Set.of(PROCESSING, REJECTED, CANCELED),
            RISK_REVIEW, Set.of(PROCESSING, REJECTED, CANCELED),
            AUTO_HANDLED, Set.of(PROCESSING),
            PROCESSING, Set.of(COMPLETED, FAILED, WAITING_HUMAN),
            WAITING_HUMAN, Set.of(PROCESSING, REJECTED, CANCELED),
            FAILED, Set.of(PROCESSING)
    );

    public String transition(String current, String target) {
        if (current == null || target == null
                || !TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessException(400, "非法状态迁移：" + current + " -> " + target);
        }
        return target;
    }
}

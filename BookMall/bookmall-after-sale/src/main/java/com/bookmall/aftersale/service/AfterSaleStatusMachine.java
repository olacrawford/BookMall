package com.bookmall.aftersale.service;

import com.bookmall.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class AfterSaleStatusMachine {


    public static final String CREATED = "CREATED";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String APPROVAL = "APPROVAL";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELED = "CANCELED";
    public static final String AUTO_HANDLED = "AUTO_HANDLED";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            CREATED, Set.of(UNDER_REVIEW),
            UNDER_REVIEW, Set.of(APPROVAL, AUTO_HANDLED, REJECTED, CANCELED),
            APPROVAL, Set.of(PROCESSING, REJECTED, CANCELED),
            PROCESSING, Set.of(COMPLETED)
    );

    public String transition(String current, String target) {
        if (current == null || target == null
                || !TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessException(400, "非法状态迁移：" + current + " -> " + target);
        }
        return target;
    }
}

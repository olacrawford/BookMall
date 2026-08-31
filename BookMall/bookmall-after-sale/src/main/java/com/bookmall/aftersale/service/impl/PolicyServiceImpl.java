package com.bookmall.aftersale.service.impl;

import com.bookmall.aftersale.dto.PolicyDecisionRequest;
import com.bookmall.aftersale.service.PolicyService;
import com.bookmall.aftersale.vo.PolicyDecision;
import com.bookmall.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PolicyServiceImpl implements PolicyService {

    private static final BigDecimal LOW_AMOUNT_THRESHOLD = new BigDecimal("50.00");

    @Override
    public PolicyDecision evaluate(PolicyDecisionRequest request) {
        if ("HIGH".equalsIgnoreCase(request.getRiskLevel())) {
            return new PolicyDecision(PolicyDecision.RISK_CONTROL, "高风险需要风控介入");
        }

        if (request.getAmount() == null) {
            throw new BusinessException(400, "金额不能为空");
        }

        if (request.getAmount().compareTo(LOW_AMOUNT_THRESHOLD) <= 0) {
            return new PolicyDecision(PolicyDecision.AUTO_REFUND, "低金额低风险自动退款");
        }

        return new PolicyDecision(PolicyDecision.REQUIRE_APPROVAL, "高金额需要人工审批");
    }
}

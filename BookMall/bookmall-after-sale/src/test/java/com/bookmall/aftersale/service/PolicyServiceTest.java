package com.bookmall.aftersale.service;

import com.bookmall.aftersale.dto.PolicyDecisionRequest;
import com.bookmall.aftersale.service.impl.PolicyServiceImpl;
import com.bookmall.aftersale.vo.PolicyDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicyServiceTest {

    private final PolicyService policyService = new PolicyServiceImpl();

    @Test
    void evaluate_autoRefund_whenLowAmountAndLowRisk() {
        PolicyDecisionRequest request = new PolicyDecisionRequest();
        request.setAmount(new BigDecimal("30.00"));
        request.setRiskLevel("LOW");

        PolicyDecision decision = policyService.evaluate(request);

        assertEquals(PolicyDecision.AUTO_REFUND, decision.getAction());
    }

    @Test
    void evaluate_requireApproval_whenHighAmountAndLowRisk() {
        PolicyDecisionRequest request = new PolicyDecisionRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setRiskLevel("LOW");

        PolicyDecision decision = policyService.evaluate(request);

        assertEquals(PolicyDecision.REQUIRE_APPROVAL, decision.getAction());
    }

    @Test
    void evaluate_riskControl_whenHighRisk() {
        PolicyDecisionRequest request = new PolicyDecisionRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setRiskLevel("HIGH");

        PolicyDecision decision = policyService.evaluate(request);

        assertEquals(PolicyDecision.RISK_CONTROL, decision.getAction());
    }
}

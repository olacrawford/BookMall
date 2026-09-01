package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.AiDecision;
import com.bookmall.aftersale.ai.model.DecisionValidation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionValidatorTest {

    private final DecisionValidator validator = new DecisionValidator();

    @Test
    void validate_validDecision_passes() {
        AiDecision decision = validDecision();

        DecisionValidation result = validator.validate(decision);

        assertTrue(result.isValid());
    }

    @Test
    void validate_illegalAction_rejects() {
        AiDecision decision = validDecision();
        decision.setAction("REFUND_DIRECT");

        DecisionValidation result = validator.validate(decision);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("action")));
    }

    @Test
    void validate_negativeAmount_rejects() {
        AiDecision decision = validDecision();
        decision.setAmount(BigDecimal.valueOf(-1));

        DecisionValidation result = validator.validate(decision);

        assertFalse(result.isValid());
    }

    @Test
    void validate_refundZeroAmount_rejects() {
        AiDecision decision = validDecision();
        decision.setAction(AiDecision.REFUND);
        decision.setAmount(BigDecimal.ZERO);

        DecisionValidation result = validator.validate(decision);

        assertFalse(result.isValid());
    }

    @Test
    void validate_emptyEvidence_rejects() {
        AiDecision decision = validDecision();
        decision.setEvidenceIds(List.of());

        DecisionValidation result = validator.validate(decision);

        assertFalse(result.isValid());
    }

    private AiDecision validDecision() {
        AiDecision decision = new AiDecision();
        decision.setIntent("LOGISTICS_NOT_RECEIVED");
        decision.setAction(AiDecision.NEEDS_HUMAN);
        decision.setAmount(BigDecimal.valueOf(39.80));
        decision.setReason("先人工核实签收凭证");
        decision.setRiskLevel("LOW");
        decision.setEvidenceIds(List.of("order:10001", "policy:v1#1"));
        decision.setPolicyVersion("v1");
        decision.setNextStep("UNDER_REVIEW");
        return decision;
    }
}

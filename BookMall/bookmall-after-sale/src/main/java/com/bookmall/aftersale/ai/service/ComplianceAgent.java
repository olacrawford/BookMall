package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.AiDecision;
import com.bookmall.aftersale.ai.model.DecisionValidation;
import org.springframework.stereotype.Component;

@Component
public class ComplianceAgent {

    private final DecisionValidator validator;

    public ComplianceAgent(DecisionValidator validator) {
        this.validator = validator;
    }

    public DecisionValidation assess(AiDecision decision) {
        return validator.validate(decision);
    }
}

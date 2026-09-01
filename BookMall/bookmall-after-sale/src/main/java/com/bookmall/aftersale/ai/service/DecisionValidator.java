package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.AiDecision;
import com.bookmall.aftersale.ai.model.DecisionValidation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DecisionValidator {

    private static final Set<String> INTENTS = Set.of(
            "LOGISTICS_NOT_RECEIVED", "DAMAGED", "MISSING_ITEM", "REFUND_REQUEST", "GENERAL_INQUIRY", "UNKNOWN");
    private static final Set<String> ACTIONS = Set.of(
            AiDecision.NO_ACTION, AiDecision.REQUEST_INFO, AiDecision.REFUND, AiDecision.COMPENSATE,
            AiDecision.NEEDS_HUMAN, AiDecision.REJECT);
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> NEXT_STEPS = Set.of(
            "UNDER_REVIEW", "WAITING_USER", "WAITING_APPROVAL", "PROCESSING", "RISK_REVIEW", "REJECTED");
    private static final Pattern POLICY_VERSION = Pattern.compile("^v[0-9]+$");

    public DecisionValidation validate(AiDecision decision) {
        List<String> errors = new ArrayList<>();
        if (decision == null) {
            return DecisionValidation.fail(List.of("Decision must not be null"));
        }
        if (decision.getIntent() == null || !INTENTS.contains(decision.getIntent())) {
            errors.add("intent is not allowed: " + decision.getIntent());
        }
        if (decision.getAction() == null || !ACTIONS.contains(decision.getAction())) {
            errors.add("action is not allowed: " + decision.getAction());
        }
        if (decision.getAmount() == null || decision.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("amount must be non-negative");
        }
        if (AiDecision.REFUND.equals(decision.getAction())
                && (decision.getAmount() == null || decision.getAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            errors.add("refund amount must be positive");
        }
        if (decision.getReason() == null || decision.getReason().isBlank()) {
            errors.add("reason must not be empty");
        } else if (decision.getReason().length() > 1000) {
            errors.add("reason must not exceed 1000 chars");
        }
        if (decision.getRiskLevel() == null || !RISK_LEVELS.contains(decision.getRiskLevel())) {
            errors.add("riskLevel is not allowed: " + decision.getRiskLevel());
        }
        if (decision.getEvidenceIds() == null || decision.getEvidenceIds().isEmpty()) {
            errors.add("evidenceIds must contain at least one item");
        } else if (decision.getEvidenceIds().size() > 20) {
            errors.add("evidenceIds must not exceed 20 items");
        } else if (decision.getEvidenceIds().stream().anyMatch(id -> id == null || id.isBlank())) {
            errors.add("evidenceIds must not contain blank items");
        }
        if (decision.getPolicyVersion() == null || !POLICY_VERSION.matcher(decision.getPolicyVersion()).matches()) {
            errors.add("policyVersion must match v[0-9]+");
        }
        if (decision.getNextStep() == null || !NEXT_STEPS.contains(decision.getNextStep())) {
            errors.add("nextStep is not allowed: " + decision.getNextStep());
        }
        return errors.isEmpty() ? DecisionValidation.ok() : DecisionValidation.fail(errors);
    }
}

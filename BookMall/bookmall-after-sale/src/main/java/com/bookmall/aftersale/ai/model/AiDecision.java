package com.bookmall.aftersale.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDecision {

    public static final String NO_ACTION = "NO_ACTION";
    public static final String REQUEST_INFO = "REQUEST_INFO";
    public static final String REFUND = "REFUND";
    public static final String COMPENSATE = "COMPENSATE";
    public static final String NEEDS_HUMAN = "NEEDS_HUMAN";
    public static final String REJECT = "REJECT";

    private String intent;
    private String action;
    private BigDecimal amount;
    private String reason;
    private String riskLevel;
    private List<String> evidenceIds = new ArrayList<>();
    private String policyVersion;
    private String nextStep;

    public static AiDecision fallbackHuman(Long ticketId, String policyVersion, String reason) {
        AiDecision decision = new AiDecision();
        decision.setIntent("UNKNOWN");
        decision.setAction(NEEDS_HUMAN);
        decision.setAmount(java.math.BigDecimal.ZERO);
        decision.setReason(reason);
        decision.setRiskLevel("MEDIUM");
        decision.setEvidenceIds(new ArrayList<>());
        decision.setPolicyVersion(policyVersion);
        decision.setNextStep("UNDER_REVIEW");
        return decision;
    }
}

package com.bookmall.aftersale.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDecision {

    public static final String AUTO_REFUND = "AUTO_REFUND";
    public static final String REQUIRE_APPROVAL = "REQUIRE_APPROVAL";
    public static final String RISK_CONTROL = "RISK_CONTROL";

    private String action;
    private String reason;
}

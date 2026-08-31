package com.bookmall.aftersale.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PolicyDecisionRequest {

    private BigDecimal amount;
    private String riskLevel;
}

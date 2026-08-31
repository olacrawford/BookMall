package com.bookmall.aftersale.service;

import com.bookmall.aftersale.dto.PolicyDecisionRequest;
import com.bookmall.aftersale.vo.PolicyDecision;

public interface PolicyService {

    PolicyDecision evaluate(PolicyDecisionRequest request);
}

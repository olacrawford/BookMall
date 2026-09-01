package com.bookmall.aftersale.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EvidenceCollection {

    private List<ToolResult> toolResults = new ArrayList<>();
    private List<RuleHit> ruleHits = new ArrayList<>();
}

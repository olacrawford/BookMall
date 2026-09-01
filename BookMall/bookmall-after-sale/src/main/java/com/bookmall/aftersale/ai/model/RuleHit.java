package com.bookmall.aftersale.ai.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RuleHit {

    private Long chunkId;
    private String documentCode;
    private String title;
    private String category;
    private String policyVersion;
    private String permissionScope;
    private String content;
    private Integer chunkNo;
    private BigDecimal score;

    public String evidenceId() {
        String version = policyVersion == null || policyVersion.isBlank() ? "v1" : policyVersion;
        String chunk = chunkNo == null ? "1" : String.valueOf(chunkNo);
        if (documentCode == null || documentCode.isBlank()) {
            return "policy:" + version + "#" + chunk;
        }
        return "policy:" + version + "#" + documentCode + "#" + chunk;
    }
}

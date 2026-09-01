package com.bookmall.aftersale.ai.rag;

import com.bookmall.aftersale.ai.model.RuleHit;

import java.util.List;

public interface RagRetriever {

    List<RuleHit> search(String query, String policyVersion, String permissionScope, int limit);
}

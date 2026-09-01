package com.bookmall.aftersale.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DecisionValidation {

    private final boolean valid;
    private final List<String> errors;

    public DecisionValidation(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors == null ? new ArrayList<>() : errors;
    }

    public static DecisionValidation ok() {
        return new DecisionValidation(true, new ArrayList<>());
    }

    public static DecisionValidation fail(List<String> errors) {
        return new DecisionValidation(false, errors);
    }
}

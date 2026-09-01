package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.AiDecision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonDecisionParser {

    private final ObjectMapper objectMapper;

    public JsonDecisionParser() {
        this.objectMapper = new ObjectMapper();
    }

    public AiDecision parse(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, AiDecision.class);
    }
}

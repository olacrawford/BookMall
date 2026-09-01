package com.bookmall.aftersale.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolInvocation {

    private String toolName;
    private String traceId;
    private Long ticketId;
    private Map<String, Object> arguments = new HashMap<>();
}

package com.bookmall.aftersale.ai.model;

import com.bookmall.aftersale.client.dto.OrderSnapshot;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TicketContext {

    private String traceId;
    private Long ticketId;
    private Long userId;
    private Long orderId;
    private String description;
    private OrderSnapshot orderSnapshot;
    private String policyVersion = "v1";
    private List<String> userEvidence = new ArrayList<>();
}

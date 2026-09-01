package com.bookmall.aftersale.ai.service;

import com.bookmall.aftersale.ai.model.TicketContext;
import org.springframework.stereotype.Component;

@Component
public class IntentAgent {

    public String extract(TicketContext context) {
        String text = context == null || context.getDescription() == null
                ? ""
                : context.getDescription();
        String query = text.toLowerCase();
        if (query.contains("签收") || query.contains("没收到") || query.contains("未收到") || query.contains("没有收到")) {
            return "LOGISTICS_NOT_RECEIVED";
        }
        if (query.contains("破损") || query.contains("坏了") || query.contains("碎裂")) {
            return "DAMAGED";
        }
        if (query.contains("少件") || query.contains("缺件") || query.contains("缺少")) {
            return "MISSING_ITEM";
        }
        if (query.contains("退款") || query.contains("退钱") || query.contains("退费")) {
            return "REFUND_REQUEST";
        }
        return "GENERAL_INQUIRY";
    }
}

package com.bookmall.aftersale.dto;

public record OutboxDeliveryMessage(String eventId, String eventType, String payload) {
}

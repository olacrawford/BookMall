package com.bookmall.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefundRequest {

    @NotBlank(message = "refundKey is required")
    private String refundKey;
}

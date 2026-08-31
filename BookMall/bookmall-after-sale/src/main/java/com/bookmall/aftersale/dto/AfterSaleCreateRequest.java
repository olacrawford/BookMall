package com.bookmall.aftersale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleCreateRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    private String orderNo;

    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "description is required")
    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "amount must not be negative")
    private BigDecimal amount;

    private String idempotencyKey;
}

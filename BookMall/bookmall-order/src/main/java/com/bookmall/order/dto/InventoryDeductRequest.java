package com.bookmall.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryDeductRequest {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量至少为1")
    private Integer quantity;

}
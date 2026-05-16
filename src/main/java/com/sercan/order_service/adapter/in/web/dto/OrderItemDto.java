package com.sercan.order_service.adapter.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemDto(
        @NotNull(message = "Product offering ID is required")
        UUID productOfferingId,

        @NotNull(message = "Quantity is required")
        @Min(1)
        int quantity
) {
}

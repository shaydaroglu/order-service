package com.sercan.order_service.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record CustomerDto(
        @NotNull(message = "Customer ID is required")
        String id
) {
}

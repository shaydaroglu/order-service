package com.sercan.order_service.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record SiteDto(
        @NotNull(message = "Site ID is required")
        String id
) {
}

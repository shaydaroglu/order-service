package com.sercan.order_service.adapter.in.web.dto;

import com.sercan.order_service.domain.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record CreateOrderRequest(
        @NotNull(message = "Category is required")
        Category category,

        @NotNull(message = "Customer is required")
        @Valid
        CustomerDto customer,

        @NotNull(message = "Site is required")
        @Valid
        SiteDto site,

        @NotEmpty(message = "Order orderItems must not be empty")
        @Valid
        List<OrderItemDto> orderItems,

        @NotNull(message = "Payment method is required")
        @Valid
        PaymentMethodDto paymentMethod
) {
}

package com.sercan.order_service.adapter.in.web.dto;

import com.sercan.order_service.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentMethodDto(
        @NotNull(message = "Payment type is required")
        PaymentMethod.PaymentType type,

        String iban
) {
}

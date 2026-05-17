package com.sercan.order_service.adapter.in.web.dto;

import com.sercan.order_service.domain.OrderState;
import jakarta.validation.Valid;
import lombok.Builder;

import java.util.List;

@Builder
public record PatchOrderRequest(
        OrderState status,

        @Valid
        List<OrderItemDto> orderItems,

        @Valid
        PaymentMethodDto paymentMethod
) {
}

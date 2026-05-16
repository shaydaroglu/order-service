package com.sercan.order_service.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        OrderState state,
        Category category,
        String customerId,
        String siteId,
        List<OrderItem> orderItems,
        PaymentMethod paymentMethod,
        String idempotencyKey,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

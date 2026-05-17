package com.sercan.order_service.domain;

import com.sercan.order_service.adapter.in.web.dto.OrderItemDto;
import com.sercan.order_service.domain.exception.InvalidOrderItemException;
import com.sercan.order_service.domain.exception.OrderNotEditableException;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public Order transitionTo(OrderState next) {
        state.validateTransition(next);
        return new Order(
                id,
                next,
                category,
                customerId,
                siteId,
                orderItems,
                paymentMethod,
                idempotencyKey,
                createdAt,
                null
        );
    }

    public Order patch(
            OrderState newState,
            List<OrderItem> newItems,
            PaymentMethod newPaymentMethod
    ) {
        if (state == OrderState.CONFIRMED) {
            throw new OrderNotEditableException("Order is confirmed and can not be modified");
        }

        if (state == OrderState.SUBMITTED && (newItems != null || newPaymentMethod != null)) {
            throw new OrderNotEditableException("Only state can be modified once order is submitted");
        }

        if (newItems != null && newItems.isEmpty()) {
            throw new InvalidOrderItemException("Order must have at least one item");
        }

        OrderState resolvedState = state;
        if (newState != null) {
            state.validateTransition(newState);
            resolvedState = newState;
        }

        return new Order(
                id,
                resolvedState,
                category,
                customerId,
                siteId,
                newItems != null ? newItems : orderItems,
                newPaymentMethod != null ? newPaymentMethod : paymentMethod,
                idempotencyKey,
                createdAt,
                null
        );
    }

    public static void validateNoDuplicateProductIds(List<OrderItemDto> items) {
        Set<UUID> seen = new HashSet<>();
        List<UUID> duplicates = items.stream()
                .map(OrderItemDto::productOfferingId)
                .filter(id -> !seen.add(id))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new InvalidOrderItemException("Duplicate product offering IDs are not allowed: " + duplicates);
        }
    }
}

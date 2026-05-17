package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.OrderNotEditableException;

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
}

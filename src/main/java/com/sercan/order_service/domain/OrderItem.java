package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.InvalidOrderItemException;

import java.util.UUID;

public record OrderItem(
        UUID productOfferingId,
        int quantity
) {
    public OrderItem {
        if (productOfferingId == null) {
            throw new InvalidOrderItemException("Product offering ID must not be null");
        }
        if (quantity < 1) {
            throw new InvalidOrderItemException("Quantity must be at least 1");
        }
    }
}

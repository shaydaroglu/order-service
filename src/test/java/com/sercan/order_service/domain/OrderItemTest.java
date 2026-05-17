package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.InvalidOrderItemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderItemTest {
    @Nested
    @DisplayName("Valid order items")
    class ValidOrderItems {

        @Test
        @DisplayName("should create order item with valid productOfferingId and quantity")
        void shouldCreateValidOrderItem() {
            assertThatNoException().isThrownBy(() ->
                    new OrderItem(UUID.randomUUID(), 1));
        }

        @Test
        @DisplayName("should create order item with quantity greater than 1")
        void shouldCreateOrderItemWithHighQuantity() {
            assertThatNoException().isThrownBy(() ->
                    new OrderItem(UUID.randomUUID(), 100));
        }
    }

    @Nested
    @DisplayName("Invalid order items")
    class InvalidOrderItems {

        @Test
        @DisplayName("should throw when productOfferingId is null")
        void shouldThrowWhenProductOfferingIdIsNull() {
            assertThatThrownBy(() -> new OrderItem(null, 1))
                    .isInstanceOf(InvalidOrderItemException.class)
                    .hasMessageContaining("Product offering ID must not be null");
        }

        @Test
        @DisplayName("should throw when quantity is zero")
        void shouldThrowWhenQuantityIsZero() {
            assertThatThrownBy(() -> new OrderItem(UUID.randomUUID(), 0))
                    .isInstanceOf(InvalidOrderItemException.class)
                    .hasMessageContaining("Quantity must be at least 1");
        }

        @Test
        @DisplayName("should throw when quantity is negative")
        void shouldThrowWhenQuantityIsNegative() {
            assertThatThrownBy(() -> new OrderItem(UUID.randomUUID(), -1))
                    .isInstanceOf(InvalidOrderItemException.class)
                    .hasMessageContaining("Quantity must be at least 1");
        }
    }
}

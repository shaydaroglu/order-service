package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.InvalidOrderItemException;
import com.sercan.order_service.domain.exception.InvalidStateTransitionException;
import com.sercan.order_service.domain.exception.OrderNotEditableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class OrderTest {

    private Order draftOrder;
    private Order submittedOrder;
    private Order confirmedOrder;
    private List<OrderItem> orderItems;
    private PaymentMethod paymentMethod;

    @BeforeEach
    void setUp() {
        orderItems = List.of(new OrderItem(UUID.randomUUID(), 2));
        paymentMethod = new PaymentMethod(PaymentMethod.PaymentType.INVOICE, null);

        draftOrder = buildOrder(OrderState.DRAFT);
        submittedOrder = buildOrder(OrderState.SUBMITTED);
        confirmedOrder = buildOrder(OrderState.CONFIRMED);
    }

    private Order buildOrder(OrderState state) {
        return new Order(
                UUID.randomUUID(),
                state,
                Category.B2B,
                "customer-1",
                "site-1",
                orderItems,
                paymentMethod,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("transitionTo")
    class TransitionTo {

        @Test
        @DisplayName("should return new order with updated state on valid transition")
        void shouldReturnNewOrderWithUpdatedState() {
            Order result = draftOrder.transitionTo(OrderState.PREVIEW);

            assertThat(result.state()).isEqualTo(OrderState.PREVIEW);
            assertThat(result.id()).isEqualTo(draftOrder.id());
            assertThat(result.category()).isEqualTo(draftOrder.category());
            assertThat(result.customerId()).isEqualTo(draftOrder.customerId());
        }

        @Test
        @DisplayName("should not mutate original order on transition")
        void shouldNotMutateOriginalOrder() {
            draftOrder.transitionTo(OrderState.PREVIEW);
            assertThat(draftOrder.state()).isEqualTo(OrderState.DRAFT);
        }

        @Test
        @DisplayName("should throw on invalid transition")
        void shouldThrowOnInvalidTransition() {
            assertThatThrownBy(() -> draftOrder.transitionTo(OrderState.CONFIRMED))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("patch")
    class Patch {

        @Test
        @DisplayName("should update state on DRAFT order")
        void shouldUpdateStateOnDraftOrder() {
            Order result = draftOrder.patch(OrderState.PREVIEW, null, null);
            assertThat(result.state()).isEqualTo(OrderState.PREVIEW);
        }

        @Test
        @DisplayName("should update orderItems on DRAFT order")
        void shouldUpdateItemsOnDraftOrder() {
            List<OrderItem> newItems = List.of(new OrderItem(UUID.randomUUID(), 5));
            Order result = draftOrder.patch(null, newItems, null);
            assertThat(result.orderItems()).isEqualTo(newItems);
        }

        @Test
        @DisplayName("should update payment method on DRAFT order")
        void shouldUpdatePaymentMethodOnDraftOrder() {
            PaymentMethod newPayment = new PaymentMethod(
                    PaymentMethod.PaymentType.DIRECT_DEBIT, "DE89370400440532013000");
            Order result = draftOrder.patch(null, null, newPayment);
            assertThat(result.paymentMethod()).isEqualTo(newPayment);
        }

        @Test
        @DisplayName("should keep existing fields when patch fields are null")
        void shouldKeepExistingFieldsWhenPatchFieldsAreNull() {
            Order result = draftOrder.patch(null, null, null);
            assertThat(result.state()).isEqualTo(draftOrder.state());
            assertThat(result.orderItems()).isEqualTo(draftOrder.orderItems());
            assertThat(result.paymentMethod()).isEqualTo(draftOrder.paymentMethod());
        }

        @Test
        @DisplayName("should throw when patching with empty orderItems list")
        void shouldThrowWhenPatchingWithEmptyItems() {
            assertThatThrownBy(() -> draftOrder.patch(null, List.of(), null))
                    .isInstanceOf(InvalidOrderItemException.class)
                    .hasMessageContaining("Order must have at least one item");
        }

        @Test
        @DisplayName("should allow state change on SUBMITTED order")
        void shouldAllowStateChangeOnSubmittedOrder() {
            Order result = submittedOrder.patch(OrderState.CONFIRMED, null, null);
            assertThat(result.state()).isEqualTo(OrderState.CONFIRMED);
        }

        @Test
        @DisplayName("should throw when patching orderItems on SUBMITTED order")
        void shouldThrowWhenPatchingItemsOnSubmittedOrder() {
            List<OrderItem> newItems = List.of(new OrderItem(UUID.randomUUID(), 3));
            assertThatThrownBy(() -> submittedOrder.patch(null, newItems, null))
                    .isInstanceOf(OrderNotEditableException.class)
                    .hasMessageContaining("Only state can be modified once order is submitted");
        }

        @Test
        @DisplayName("should throw when patching payment method on SUBMITTED order")
        void shouldThrowWhenPatchingPaymentMethodOnSubmittedOrder() {
            PaymentMethod newPayment = new PaymentMethod(PaymentMethod.PaymentType.INVOICE, null);
            assertThatThrownBy(() -> submittedOrder.patch(null, null, newPayment))
                    .isInstanceOf(OrderNotEditableException.class)
                    .hasMessageContaining("Only state can be modified once order is submitted");
        }

        @Test
        @DisplayName("should throw when patching CONFIRMED order")
        void shouldThrowWhenPatchingConfirmedOrder() {
            assertThatThrownBy(() -> confirmedOrder.patch(null, null, null))
                    .isInstanceOf(OrderNotEditableException.class)
                    .hasMessageContaining("Order is confirmed and can not be modified");
        }

        @Test
        @DisplayName("should throw on invalid state transition in patch")
        void shouldThrowOnInvalidStateTransitionInPatch() {
            assertThatThrownBy(() -> draftOrder.patch(OrderState.CONFIRMED, null, null))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("should not mutate original order on patch")
        void shouldNotMutateOriginalOrder() {
            draftOrder.patch(OrderState.PREVIEW, null, null);
            assertThat(draftOrder.state()).isEqualTo(OrderState.DRAFT);
        }
    }
}

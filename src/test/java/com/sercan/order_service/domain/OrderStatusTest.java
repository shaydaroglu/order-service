package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderStatusTest {
    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("DRAFT → PREVIEW should be allowed")
        void draftToPreview() {
            assertThatNoException().isThrownBy(() -> OrderState.DRAFT.validateTransition(OrderState.PREVIEW));
        }

        @Test
        @DisplayName("PREVIEW → DRAFT should be allowed")
        void previewToDraft() {
            assertThatNoException().isThrownBy(() -> OrderState.PREVIEW.validateTransition(OrderState.DRAFT));
        }

        @Test
        @DisplayName("PREVIEW → SUBMITTED should be allowed")
        void previewToSubmitted() {
            assertThatNoException().isThrownBy(() -> OrderState.PREVIEW.validateTransition(OrderState.SUBMITTED));
        }

        @Test
        @DisplayName("SUBMITTED → CONFIRMED should be allowed")
        void submittedToConfirmed() {
            assertThatNoException().isThrownBy(() -> OrderState.SUBMITTED.validateTransition(OrderState.CONFIRMED));
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("DRAFT → SUBMITTED should be rejected")
        void draftToSubmitted() {
            assertThatThrownBy(() -> OrderState.DRAFT.validateTransition(OrderState.SUBMITTED))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("DRAFT → CONFIRMED should be rejected")
        void draftToConfirmed() {
            assertThatThrownBy(() -> OrderState.DRAFT.validateTransition(OrderState.CONFIRMED))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("PREVIEW → CONFIRMED should be rejected")
        void previewToConfirmed() {
            assertThatThrownBy(() -> OrderState.PREVIEW.validateTransition(OrderState.CONFIRMED))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("SUBMITTED → DRAFT should be rejected")
        void submittedToDraft() {
            assertThatThrownBy(() -> OrderState.SUBMITTED.validateTransition(OrderState.DRAFT))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("SUBMITTED → PREVIEW should be rejected")
        void submittedToPreview() {
            assertThatThrownBy(() -> OrderState.SUBMITTED.validateTransition(OrderState.PREVIEW))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("CONFIRMED → any state should be rejected")
        void confirmedToAnyState() {
            for (OrderState state : OrderState.values()) {
                assertThatThrownBy(() -> OrderState.CONFIRMED.validateTransition(state))
                        .isInstanceOf(InvalidStateTransitionException.class);
            }
        }
    }
}

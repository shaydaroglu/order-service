package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.InvalidStateTransitionException;

import java.util.Map;
import java.util.Set;

public enum OrderState {
    DRAFT,
    PREVIEW,
    SUBMITTED,
    CONFIRMED;

    private static final Map<OrderState, Set<OrderState>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT,      Set.of(PREVIEW),
            PREVIEW,    Set.of(DRAFT, SUBMITTED),
            SUBMITTED,  Set.of(CONFIRMED),
            CONFIRMED,  Set.of()
    );

    public void validateTransition(OrderState next) {
        if (!ALLOWED_TRANSITIONS.get(this).contains(next)) {
            throw new InvalidStateTransitionException(this, next);
        }
    }
}

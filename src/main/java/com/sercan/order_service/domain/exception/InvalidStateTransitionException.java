package com.sercan.order_service.domain.exception;

import com.sercan.order_service.domain.OrderState;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(OrderState from, OrderState to) {
        super("Invalid state transition from " + from + " to " + to);
    }
}

package com.sercan.order_service.domain.exception;

import com.sercan.order_service.domain.OrderStatus;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid state transition from " + from + " to " + to);
    }
}

package com.sercan.order_service.domain.exception;

import com.sercan.order_service.domain.Order;
import lombok.Getter;

@Getter
public class IdempotencyReplayException extends RuntimeException {
    private final Order order;

    public IdempotencyReplayException(Order order) {
        super("Idempotency replay");
        this.order = order;
    }

}

package com.sercan.order_service.domain.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency conflict: a different request was already processed with key " + idempotencyKey);
    }
}

package com.sercan.order_service.domain.exception;

public class InvalidOrderItemException extends RuntimeException {
    public InvalidOrderItemException(String message) {
        super(message);
    }
}

package com.sercan.order_service.domain.exception;

public class InvalidProductOfferingException extends RuntimeException {
    public InvalidProductOfferingException(String message) {
        super(message);
    }
}

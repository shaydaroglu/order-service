package com.sercan.order_service.adapter.in.web;

import com.sercan.order_service.adapter.in.web.dto.ErrorResponse;
import com.sercan.order_service.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Order not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Order Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
            InvalidStateTransitionException ex,
            HttpServletRequest request) {
        log.warn("Invalid state transition: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid State Transition", ex.getMessage(), request);
    }

    @ExceptionHandler(OrderNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotEditable(
            OrderNotEditableException ex,
            HttpServletRequest request) {
        log.warn("Order not editable: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Order Not Editable", ex.getMessage(), request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException ex,
            HttpServletRequest request) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Idempotency Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPaymentMethodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentMethod(
            InvalidPaymentMethodException ex,
            HttpServletRequest request) {
        log.warn("Invalid payment method: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid Payment Method", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidOrderItemException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderItem(
            InvalidOrderItemException ex,
            HttpServletRequest request) {
        log.warn("Invalid order item: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Invalid Order Item", ex.getMessage(), request);
    }

    @ExceptionHandler(CatalogServiceException.class)
    public ResponseEntity<ErrorResponse> handleCatalogServiceException(
            CatalogServiceException ex,
            HttpServletRequest request) {
        log.error("Catalog service error: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Catalog Service Unavailable", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Objects.requireNonNullElse(fe.getDefaultMessage(), "Invalid value"),
                        (a, b) -> a
                ));
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ErrorResponse.of(
                        "Validation Failed",
                        HttpStatus.BAD_REQUEST.value(),
                        "One or more fields are invalid",
                        URI.create(request.getRequestURI()),
                        Map.of("fieldErrors", fieldErrors)
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Unreadable request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", "Request body is missing or malformed", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        log.warn("No resource found: {}", request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ErrorResponse.of(status.value(), title, detail, URI.create(request.getRequestURI())));
    }

}

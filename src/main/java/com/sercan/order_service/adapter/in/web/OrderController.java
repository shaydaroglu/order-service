package com.sercan.order_service.adapter.in.web;

import com.sercan.order_service.adapter.in.web.dto.CreateOrderRequest;
import com.sercan.order_service.adapter.in.web.dto.OrderResponse;
import com.sercan.order_service.adapter.in.web.dto.PageResponse;
import com.sercan.order_service.adapter.in.web.dto.PatchOrderRequest;
import com.sercan.order_service.application.port.in.OrderUseCase;
import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.exception.IdempotencyReplayException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer-orders")
@RequiredArgsConstructor
@Validated
@Slf4j
public class OrderController {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private final OrderUseCase orderUseCase;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody @Valid CreateOrderRequest createOrderRequest,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey
    ) {
        log.info("POST /api/v1/customer-orders idempotencyKey={}", idempotencyKey);
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(OrderResponse.from(orderUseCase.createOrder(createOrderRequest, idempotencyKey)));
        } catch (IdempotencyReplayException ex) {
            log.info("Replaying existing order for idempotencyKey={}", idempotencyKey);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .header("X-Idempotency-Replayed", "true")
                    .body(OrderResponse.from(ex.getOrder()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        log.info("GET /api/v1/customer-orders orderId={}", id);
        return ResponseEntity.ok(OrderResponse.from(orderUseCase.getOrder(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> listOrders(
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        log.info("GET /api/v1/customer-orders category={} page={} size={}", category, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(PageResponse.from(orderUseCase.listOrders(category, pageable).map(OrderResponse::from)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable UUID id,
            @RequestBody @Valid PatchOrderRequest patchRequest
    ) {
        log.info("PATCH /api/v1/customer-orders id={}", id);
        return ResponseEntity.ok(OrderResponse.from(orderUseCase.patchOrder(id, patchRequest)));
    }
}

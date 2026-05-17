package com.sercan.order_service.adapter.in.web;

import com.sercan.order_service.adapter.in.web.dto.*;
import com.sercan.order_service.application.port.in.OrderUseCase;
import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.exception.IdempotencyReplayException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Customer Orders", description = "API for managing customer orders lifecycle")
public class OrderController {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private final OrderUseCase orderUseCase;

    @PostMapping
    @Operation(
            summary = "Create a new order",
            description = "Creates a new customer order in DRAFT state. Supports idempotent creation via the Idempotency-Key header."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Idempotency replay — order already exists with the same key and payload",
                    headers = @Header(name = "Idempotency-Replayed", description = "Present and true on replay", schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or business rule violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Idempotency key conflict — same key used with different payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Catalog service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
                    .header("Idempotency-Replayed", "true")
                    .body(OrderResponse.from(ex.getOrder()));
        }
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieves a single customer order by its UUID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order found",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        log.info("GET /api/v1/customer-orders orderId={}", id);
        return ResponseEntity.ok(OrderResponse.from(orderUseCase.getOrder(id)));
    }

    @Operation(
            summary = "List orders",
            description = "Returns a paginated list of customer orders with an optional category filter."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Paginated list of orders"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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

    @Operation(
            summary = "Partially update an order",
            description = """
                    Applies a JSON Merge Patch to an existing order.
                    Fields present in the body are updated; absent fields are left unchanged.
                    
                    State transition rules:
                    - DRAFT → PREVIEW
                    - PREVIEW → DRAFT (revert)
                    - PREVIEW → SUBMITTED
                    - SUBMITTED → CONFIRMED
                    
                    Once SUBMITTED, only the state field can be modified.
                    Once CONFIRMED, no further changes are allowed.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid state transition or business rule violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Catalog service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable UUID id,
            @RequestBody @Valid PatchOrderRequest patchRequest
    ) {
        log.info("PATCH /api/v1/customer-orders id={}", id);
        return ResponseEntity.ok(OrderResponse.from(orderUseCase.patchOrder(id, patchRequest)));
    }
}

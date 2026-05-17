package com.sercan.order_service.application.service;

import com.sercan.order_service.adapter.in.web.dto.*;
import com.sercan.order_service.application.port.out.CatalogValidationPort;
import com.sercan.order_service.application.port.out.OrderRepository;
import com.sercan.order_service.domain.*;
import com.sercan.order_service.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogValidationPort catalogValidationPort;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private Order draftOrder;
    private Order previewOrder;
    private Order submittedOrder;
    private Order confirmedOrder;
    private CreateOrderRequest createRequest;
    private UUID productOfferingId1;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productOfferingId1 = UUID.randomUUID();

        createRequest = new CreateOrderRequest(
                Category.B2B,
                new CustomerDto("customer-1"),
                new SiteDto("site-1"),
                List.of(new OrderItemDto(productOfferingId1, 2)),
                new PaymentMethodDto(PaymentMethod.PaymentType.INVOICE, null)
        );

        draftOrder = buildOrder(orderId, OrderState.DRAFT);
        previewOrder = buildOrder(orderId, OrderState.PREVIEW);
        submittedOrder = buildOrder(orderId, OrderState.SUBMITTED);
        confirmedOrder = buildOrder(orderId, OrderState.CONFIRMED);
    }

    private Order buildOrder(UUID id, OrderState state) {
        return new Order(
                id,
                state,
                Category.B2B,
                "customer-1",
                "site-1",
                List.of(new OrderItem(productOfferingId1, 2)),
                new PaymentMethod(PaymentMethod.PaymentType.INVOICE, null),
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("should create order successfully without idempotency key")
        void shouldCreateOrderSuccessfully() {
            when(orderRepository.save(any())).thenReturn(draftOrder);

            Order result = orderService.createOrder(createRequest, null);

            assertThat(result).isNotNull();
            assertThat(result.state()).isEqualTo(OrderState.DRAFT);
            assertThat(result.category()).isEqualTo(Category.B2B);
            verify(catalogValidationPort).validateProductOfferings(List.of(productOfferingId1));
            verify(orderRepository).save(any());
        }

        @Test
        @DisplayName("should create order successfully with idempotency key")
        void shouldCreateOrderWithIdempotencyKey() {
            String key = UUID.randomUUID().toString();
            when(orderRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());
            when(orderRepository.save(any())).thenReturn(draftOrder);

            Order result = orderService.createOrder(createRequest, key);

            assertThat(result).isNotNull();
            verify(orderRepository).findByIdempotencyKey(key);
            verify(orderRepository).save(any());
        }

        @Test
        @DisplayName("should replay existing order when idempotency key matches identical payload")
        void shouldReplayExistingOrderOnIdenticalPayload() {
            String key = UUID.randomUUID().toString();
            Order existingOrder = new Order(
                    orderId, OrderState.DRAFT, Category.B2B,
                    "customer-1", "site-1",
                    List.of(new OrderItem(productOfferingId1, 2)),
                    new PaymentMethod(PaymentMethod.PaymentType.INVOICE, null),
                    key, OffsetDateTime.now(), OffsetDateTime.now()
            );
            when(orderRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingOrder));

            assertThatThrownBy(() -> orderService.createOrder(createRequest, key))
                    .isInstanceOf(IdempotencyReplayException.class)
                    .satisfies(ex -> {
                        IdempotencyReplayException e = (IdempotencyReplayException) ex;
                        assertThat(e.getOrder().id()).isEqualTo(orderId);
                    });

            verify(orderRepository, never()).save(any());
            verify(catalogValidationPort, never()).validateProductOfferings(any());
        }

        @Test
        @DisplayName("should throw IdempotencyConflictException when same key has different payload")
        void shouldThrowOnIdempotencyConflict() {
            String key = UUID.randomUUID().toString();
            Order existingOrder = new Order(
                    orderId, OrderState.DRAFT, Category.B2C,
                    "different-customer", "different-site",
                    List.of(new OrderItem(productOfferingId1, 2)),
                    new PaymentMethod(PaymentMethod.PaymentType.INVOICE, null),
                    key, OffsetDateTime.now(), OffsetDateTime.now()
            );
            when(orderRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existingOrder));

            assertThatThrownBy(() -> orderService.createOrder(createRequest, key))
                    .isInstanceOf(IdempotencyConflictException.class);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when catalog validation fails")
        void shouldThrowWhenCatalogValidationFails() {
            doThrow(new CatalogServiceException("Invalid product offerings"))
                    .when(catalogValidationPort).validateProductOfferings(any());

            assertThatThrownBy(() -> orderService.createOrder(createRequest, null))
                    .isInstanceOf(CatalogServiceException.class);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when payment method is invalid")
        void shouldThrowWhenPaymentMethodIsInvalid() {
            CreateOrderRequest invalidRequest = new CreateOrderRequest(
                    Category.B2B,
                    new CustomerDto("customer-1"),
                    new SiteDto("site-1"),
                    List.of(new OrderItemDto(productOfferingId1, 2)),
                    new PaymentMethodDto(PaymentMethod.PaymentType.DIRECT_DEBIT, null)
            );

            assertThatThrownBy(() -> orderService.createOrder(invalidRequest, null))
                    .isInstanceOf(InvalidPaymentMethodException.class);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when create request has duplicate product offering ids")
        void shouldThrowWhenCreateHasDuplicateProductIds() {
            CreateOrderRequest invalidRequest = new CreateOrderRequest(
                    Category.B2B,
                    new CustomerDto("customer-1"),
                    new SiteDto("site-1"),
                    List.of(
                            new OrderItemDto(productOfferingId1, 2),
                            new OrderItemDto(productOfferingId1, 3)
                    ),
                    new PaymentMethodDto(PaymentMethod.PaymentType.INVOICE, null)
            );

            assertThatThrownBy(() -> orderService.createOrder(invalidRequest, null))
                    .isInstanceOf(InvalidOrderItemException.class)
                    .hasMessageContaining("Duplicate product offering IDs are not allowed");
        }
    }

    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrderWhenFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));

            Order result = orderService.getOrder(orderId);

            assertThat(result.id()).isEqualTo(orderId);
            verify(orderRepository).findById(orderId);
        }

        @Test
        @DisplayName("should throw OrderNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrder(orderId))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listOrders")
    class ListOrders {

        @Test
        @DisplayName("should return paginated orders")
        void shouldReturnPaginatedOrders() {
            var pageable = PageRequest.of(0, 20);
            var page = new PageImpl<>(List.of(draftOrder, previewOrder));
            when(orderRepository.findAll(null, pageable)).thenReturn(page);

            var result = orderService.listOrders(null, pageable);

            assertThat(result.getContent()).hasSize(2);
            verify(orderRepository).findAll(null, pageable);
        }

        @Test
        @DisplayName("should filter orders by category")
        void shouldFilterByCategory() {
            var pageable = PageRequest.of(0, 20);
            var page = new PageImpl<>(List.of(draftOrder));
            when(orderRepository.findAll(Category.B2B, pageable)).thenReturn(page);

            var result = orderService.listOrders(Category.B2B, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).findAll(Category.B2B, pageable);
        }
    }

    @Nested
    @DisplayName("patchOrder")
    class PatchOrder {

        @Test
        @DisplayName("should patch state successfully")
        void shouldPatchStateSuccessfully() {
            PatchOrderRequest request = new PatchOrderRequest(OrderState.PREVIEW, null, null);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));
            when(orderRepository.save(any())).thenReturn(previewOrder);

            Order result = orderService.patchOrder(orderId, request);

            assertThat(result.state()).isEqualTo(OrderState.PREVIEW);
            verify(orderRepository).save(any());
        }

        @Test
        @DisplayName("should only validate new product offering ids on patch")
        void shouldOnlyValidateNewProductOfferingIds() {
            UUID newProductId = UUID.randomUUID();
            PatchOrderRequest request = new PatchOrderRequest(
                    null,
                    List.of(
                            new OrderItemDto(productOfferingId1, 2),
                            new OrderItemDto(newProductId, 1)
                    ),
                    null
            );
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));
            when(orderRepository.save(any())).thenReturn(draftOrder);

            orderService.patchOrder(orderId, request);

            verify(catalogValidationPort).validateProductOfferings(List.of(newProductId));
        }

        @Test
        @DisplayName("should throw when patch request has duplicate product offering ids")
        void shouldThrowWhenPatchHasDuplicateProductIds() {
            PatchOrderRequest request = new PatchOrderRequest(
                    null,
                    List.of(
                            new OrderItemDto(productOfferingId1, 2),
                            new OrderItemDto(productOfferingId1, 3)
                    ),
                    null
            );
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));

            assertThatThrownBy(() -> orderService.patchOrder(orderId, request))
                    .isInstanceOf(InvalidOrderItemException.class)
                    .hasMessageContaining("Duplicate product offering IDs are not allowed");
        }

        @Test
        @DisplayName("should not call catalog when all product offering ids already exist")
        void shouldNotCallCatalogWhenNoNewIds() {
            PatchOrderRequest request = new PatchOrderRequest(
                    null,
                    List.of(new OrderItemDto(productOfferingId1, 5)),
                    null
            );
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));
            when(orderRepository.save(any())).thenReturn(draftOrder);

            orderService.patchOrder(orderId, request);

            verify(catalogValidationPort, never()).validateProductOfferings(any());
        }

        @Test
        @DisplayName("should throw OrderNotFoundException when order not found on patch")
        void shouldThrowWhenOrderNotFound() {
            PatchOrderRequest request = new PatchOrderRequest(OrderState.PREVIEW, null, null);
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.patchOrder(orderId, request))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when patching confirmed order")
        void shouldThrowWhenPatchingConfirmedOrder() {
            PatchOrderRequest request = new PatchOrderRequest(null, null, null);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));

            assertThatThrownBy(() -> orderService.patchOrder(orderId, request))
                    .isInstanceOf(OrderNotEditableException.class);
        }

        @Test
        @DisplayName("should throw when patching orderItems on submitted order")
        void shouldThrowWhenPatchingItemsOnSubmittedOrder() {
            PatchOrderRequest request = new PatchOrderRequest(
                    null,
                    List.of(new OrderItemDto(UUID.randomUUID(), 1)),
                    null
            );
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(submittedOrder));

            assertThatThrownBy(() -> orderService.patchOrder(orderId, request))
                    .isInstanceOf(OrderNotEditableException.class);
        }

        @Test
        @DisplayName("should throw when catalog validation fails on patch")
        void shouldThrowWhenCatalogValidationFailsOnPatch() {
            UUID newProductId = UUID.randomUUID();
            PatchOrderRequest request = new PatchOrderRequest(
                    null,
                    List.of(new OrderItemDto(newProductId, 1)),
                    null
            );
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(draftOrder));
            doThrow(new CatalogServiceException("Invalid product offerings"))
                    .when(catalogValidationPort).validateProductOfferings(any());

            assertThatThrownBy(() -> orderService.patchOrder(orderId, request))
                    .isInstanceOf(CatalogServiceException.class);

            verify(orderRepository, never()).save(any());
        }
    }
}

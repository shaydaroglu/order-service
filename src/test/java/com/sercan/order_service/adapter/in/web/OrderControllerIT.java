package com.sercan.order_service.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sercan.order_service.adapter.in.web.dto.*;
import com.sercan.order_service.adapter.out.persistence.OrderJpaRepository;
import com.sercan.order_service.application.port.out.CatalogValidationPort;
import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.OrderState;
import com.sercan.order_service.domain.PaymentMethod;
import com.sercan.order_service.domain.exception.CatalogServiceException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderJpaRepository jpaRepository;

    @MockitoBean
    private CatalogValidationPort catalogValidationPort;

    private UUID productOfferingId1;
    private UUID productOfferingId2;
    private CreateOrderRequest validRequest;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();

        productOfferingId1 = UUID.randomUUID();
        productOfferingId2 = UUID.randomUUID();

        validRequest = new CreateOrderRequest(
                Category.B2B,
                new CustomerDto("customer-1"),
                new SiteDto("site-1"),
                List.of(new OrderItemDto(productOfferingId1, 2)),
                new PaymentMethodDto(PaymentMethod.PaymentType.INVOICE, null)
        );
    }

    @Nested
    @DisplayName("POST /api/v1/customer-orders")
    class CreateOrder {

        @Test
        @DisplayName("should create order and return 201")
        void shouldCreateOrderAndReturn201() throws Exception {
            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.state").value("DRAFT"))
                    .andExpect(jsonPath("$.category").value("B2B"))
                    .andExpect(jsonPath("$.customer.id").value("customer-1"))
                    .andExpect(jsonPath("$.site.id").value("site-1"))
                    .andExpect(jsonPath("$.orderItems", hasSize(1)))
                    .andExpect(jsonPath("$.paymentMethod.type").value("INVOICE"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when category is missing")
        void shouldReturn400WhenCategoryMissing() throws Exception {
            CreateOrderRequest invalidRequest = new CreateOrderRequest(
                    null,
                    new CustomerDto("customer-1"),
                    new SiteDto("site-1"),
                    List.of(new OrderItemDto(productOfferingId1, 2)),
                    new PaymentMethodDto(PaymentMethod.PaymentType.INVOICE, null)
            );

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/problem+json"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors.fieldErrors.category").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when order items are empty")
        void shouldReturn400WhenOrderItemsEmpty() throws Exception {
            CreateOrderRequest invalidRequest = new CreateOrderRequest(
                    Category.B2B,
                    new CustomerDto("customer-1"),
                    new SiteDto("site-1"),
                    List.of(),
                    new PaymentMethodDto(PaymentMethod.PaymentType.INVOICE, null)
            );

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fieldErrors.orderItems").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when payment method is missing")
        void shouldReturn400WhenPaymentMethodMissing() throws Exception {
            CreateOrderRequest invalidRequest = new CreateOrderRequest(
                    Category.B2B,
                    new CustomerDto("customer-1"),
                    new SiteDto("site-1"),
                    List.of(new OrderItemDto(productOfferingId1, 2)),
                    null
            );

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fieldErrors.paymentMethod").isNotEmpty());
        }

        @Test
        @DisplayName("should return 400 when DIRECT_DEBIT has no IBAN")
        void shouldReturn400WhenDirectDebitHasNoIban() throws Exception {
            CreateOrderRequest invalidRequest = new CreateOrderRequest(
                    Category.B2B,
                    new CustomerDto("customer-1"),
                    new SiteDto("site-1"),
                    List.of(new OrderItemDto(productOfferingId1, 2)),
                    new PaymentMethodDto(PaymentMethod.PaymentType.DIRECT_DEBIT, null)
            );

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 502 when catalog service is unavailable")
        void shouldReturn502WhenCatalogUnavailable() throws Exception {
            doThrow(new CatalogServiceException("Catalog service is unavailable"))
                    .when(catalogValidationPort).validateProductOfferings(any());

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502));
        }

        @Test
        @DisplayName("should return 200 with X-Idempotency-Replayed header on replay")
        void shouldReturn200OnIdempotencyReplay() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", idempotencyKey)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", idempotencyKey)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Idempotency-Replayed", "true"));
        }

        @Test
        @DisplayName("should return 409 on idempotency conflict")
        void shouldReturn409OnIdempotencyConflict() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", idempotencyKey)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            CreateOrderRequest differentRequest = new CreateOrderRequest(
                    Category.B2C,
                    new CustomerDto("different-customer"),
                    new SiteDto("different-site"),
                    List.of(new OrderItemDto(productOfferingId2, 3)),
                    new PaymentMethodDto(PaymentMethod.PaymentType.INVOICE, null)
            );

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", idempotencyKey)
                            .content(objectMapper.writeValueAsString(differentRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/customer-orders/{id}")
    class GetOrder {

        @Test
        @DisplayName("should return order when found")
        void shouldReturnOrderWhenFound() throws Exception {
            MvcResult createResult = mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            mockMvc.perform(get("/api/v1/customer-orders/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.state").value("DRAFT"));
        }

        @Test
        @DisplayName("should return 404 when order not found")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/customer-orders/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/problem+json"))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/customer-orders")
    class ListOrders {

        @Test
        @DisplayName("should return paginated orders")
        void shouldReturnPaginatedOrders() throws Exception {
            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/customer-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("should filter orders by category")
        void shouldFilterByCategory() throws Exception {
            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            CreateOrderRequest b2cRequest = new CreateOrderRequest(
                    Category.B2C,
                    new CustomerDto("customer-2"),
                    new SiteDto("site-2"),
                    List.of(new OrderItemDto(productOfferingId1, 1)),
                    new PaymentMethodDto(PaymentMethod.PaymentType.INVOICE, null)
            );

            mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(b2cRequest)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/customer-orders")
                            .param("category", "B2B"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].category").value("B2B"));
        }

        @Test
        @DisplayName("should return 400 when page is negative")
        void shouldReturn400WhenPageIsNegative() throws Exception {
            mockMvc.perform(get("/api/v1/customer-orders")
                            .param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when size exceeds maximum")
        void shouldReturn400WhenSizeExceedsMax() throws Exception {
            mockMvc.perform(get("/api/v1/customer-orders")
                            .param("size", "101"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/customer-orders/{id}")
    class PatchOrder {

        private String createOrder() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/customer-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();
            return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
        }

        @Test
        @DisplayName("should transition state from DRAFT to PREVIEW")
        void shouldTransitionDraftToPreview() throws Exception {
            String id = createOrder();
            PatchOrderRequest request = new PatchOrderRequest(OrderState.PREVIEW, null, null);

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("PREVIEW"));
        }

        @Test
        @DisplayName("should transition full lifecycle DRAFT → PREVIEW → SUBMITTED → CONFIRMED")
        void shouldTransitionFullLifecycle() throws Exception {
            String id = createOrder();

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PatchOrderRequest(OrderState.PREVIEW, null, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("PREVIEW"));

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PatchOrderRequest(OrderState.SUBMITTED, null, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("SUBMITTED"));

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PatchOrderRequest(OrderState.CONFIRMED, null, null))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.state").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should return 400 on invalid state transition")
        void shouldReturn400OnInvalidStateTransition() throws Exception {
            String id = createOrder();
            PatchOrderRequest request = new PatchOrderRequest(OrderState.CONFIRMED, null, null);

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 400 when patching with empty items list")
        void shouldReturn400WhenPatchingWithEmptyItems() throws Exception {
            String id = createOrder();
            PatchOrderRequest request = new PatchOrderRequest(null, List.of(), null);

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 400 when patching items on SUBMITTED order")
        void shouldReturn400WhenPatchingItemsOnSubmittedOrder() throws Exception {
            String id = createOrder();

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PatchOrderRequest(OrderState.PREVIEW, null, null))))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PatchOrderRequest(OrderState.SUBMITTED, null, null))))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PatchOrderRequest(null,
                                            List.of(new OrderItemDto(productOfferingId2, 1)),
                                            null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 400 when patching CONFIRMED order")
        void shouldReturn400WhenPatchingConfirmedOrder() throws Exception {
            String id = createOrder();

            List.of(OrderState.PREVIEW, OrderState.SUBMITTED, OrderState.CONFIRMED).forEach(state -> {
                try {
                    mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            new PatchOrderRequest(state, null, null))))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new PatchOrderRequest(null, null, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("should return 404 when order not found")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            PatchOrderRequest request = new PatchOrderRequest(OrderState.PREVIEW, null, null);

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should update order items on DRAFT order")
        void shouldUpdateOrderItemsOnDraftOrder() throws Exception {
            String id = createOrder();
            PatchOrderRequest request = new PatchOrderRequest(
                    null,
                    List.of(new OrderItemDto(productOfferingId1, 5)),
                    null
            );

            mockMvc.perform(patch("/api/v1/customer-orders/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderItems[0].quantity").value(5));
        }
    }
}

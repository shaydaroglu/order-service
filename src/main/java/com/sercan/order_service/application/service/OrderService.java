package com.sercan.order_service.application.service;

import com.sercan.order_service.adapter.in.web.dto.CreateOrderRequest;
import com.sercan.order_service.adapter.in.web.dto.OrderItemDto;
import com.sercan.order_service.adapter.in.web.dto.PatchOrderRequest;
import com.sercan.order_service.application.port.in.OrderUseCase;
import com.sercan.order_service.application.port.out.CatalogValidationPort;
import com.sercan.order_service.application.port.out.OrderRepository;
import com.sercan.order_service.domain.*;
import com.sercan.order_service.domain.exception.IdempotencyConflictException;
import com.sercan.order_service.domain.exception.IdempotencyReplayException;
import com.sercan.order_service.domain.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService implements OrderUseCase {
    private final OrderRepository orderRepository;
    private final CatalogValidationPort catalogValidationPort;

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request, String idempotencyKey) {
        log.info("Creating order category={}, idempotencyKey={}",request.category(), idempotencyKey);

        checkIdempotency(idempotencyKey, request);
        List<UUID> productOfferingsIds = request.orderItems().stream()
                .map(OrderItemDto::productOfferingId)
                .toList();

        log.debug("Validating {} product offerings", productOfferingsIds.size());
        catalogValidationPort.validateProductOfferings(productOfferingsIds);

        PaymentMethod paymentMethod = new PaymentMethod(
                request.paymentMethod().type(),
                request.paymentMethod().iban()
        );
        paymentMethod.validate();

        List<OrderItem> orderItems = request.orderItems().stream()
                .map(item -> new OrderItem(item.productOfferingId(), item.quantity()))
                .toList();

        Order order = new Order(
                null,
                OrderState.DRAFT,
                request.category(),
                request.customer().id(),
                request.site().id(),
                orderItems,
                paymentMethod,
                idempotencyKey,
                null,
                null
        );

        try {
            Order saved = orderRepository.save(order);
            log.info("Order created id={}", saved.id());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent idempotency key conflict for key={}", idempotencyKey);
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IdempotencyConflictException(idempotencyKey));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        log.info("Getting order with id={}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> listOrders(Category category, Pageable pageable) {
        log.info("Listing orders. page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findAll(category, pageable);
    }

    @Override
    @Transactional
    public Order patchOrder(UUID id, PatchOrderRequest request) {
        log.info("Updating order with id={}", id);
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id.toString()));

        List<OrderItem> newItems = null;
        if(request.items() != null) {
            validateNewProductOfferingsIds(existing, request.items());

            newItems = request.items().stream()
                    .map(item -> new OrderItem(item.productOfferingId(), item.quantity()))
                    .toList();
        }

        PaymentMethod paymentMethod = null;
        if(request.paymentMethod() != null) {
            paymentMethod = new PaymentMethod(
                    request.paymentMethod().type(),
                    request.paymentMethod().iban()
            );
            paymentMethod.validate();
        }

        Order patchedOrder = existing.patch(
                request.status(),
                newItems,
                paymentMethod);

        Order saved = orderRepository.save(patchedOrder);
        log.info("Order patched id={} state={}", saved.id(), saved.state());
        return saved;
    }

    private void validateNewProductOfferingsIds(Order existingOrder, List<OrderItemDto> patchItems) {
        Set<UUID> existingIds = existingOrder.orderItems().stream()
                .map(OrderItem::productOfferingId)
                .collect(Collectors.toSet());

        List<UUID> addedIds = patchItems.stream()
                .map(OrderItemDto::productOfferingId)
                .filter(i -> !existingIds.contains(i))
                .toList();
        if(!addedIds.isEmpty()) {
            log.debug("Validating {} new product offerings on patch", addedIds.size());
            catalogValidationPort.validateProductOfferings(addedIds);
        }
    }

    private void checkIdempotency(String idempotencyKey, CreateOrderRequest request) {
        if (StringUtils.isBlank(idempotencyKey)) return;

        orderRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            if (!isPayloadIdentical(existing, request)) {
                log.warn("Idempotency conflict for key={}", idempotencyKey);
                throw new IdempotencyConflictException(idempotencyKey);
            }
            log.info("Replaying existing order for idempotencyKey={}", idempotencyKey);
            throw new IdempotencyReplayException(existing);
        });
    }

    private boolean isPayloadIdentical(Order existing, CreateOrderRequest request) {
        if (existing.category() != request.category()) return false;
        if (!Objects.equals(existing.customerId(), request.customer().id())) return false;
        if (!Objects.equals(existing.siteId(), request.site().id())) return false;
        if (existing.paymentMethod().type() != request.paymentMethod().type()) return false;
        if (!Objects.equals(existing.paymentMethod().iban(), request.paymentMethod().iban())) return false;

        if (existing.orderItems().size() != request.orderItems().size()) return false;

        Map<UUID, Integer> existingItems = existing.orderItems().stream()
                .collect(Collectors.toMap(
                        OrderItem::productOfferingId,
                        OrderItem::quantity
                ));

        return request.orderItems().stream().allMatch(dto ->
                Objects.equals(existingItems.get(dto.productOfferingId()), dto.quantity())
        );
    }
}

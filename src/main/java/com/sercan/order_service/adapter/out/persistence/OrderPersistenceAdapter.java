package com.sercan.order_service.adapter.out.persistence;

import com.sercan.order_service.application.port.out.OrderRepository;
import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.Order;
import com.sercan.order_service.domain.exception.OrderNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;
    private final EntityManager entityManager;

    @Override
    public Order save(Order order) {
        log.debug("Saving order id={}", order.id());

        OrderEntity entity = order.id() == null
                ? mapper.toEntity(order)
                : jpaRepository.findById(order.id())
                .map(existing -> updateEntity(existing, order))
                .orElseThrow(() -> new OrderNotFoundException(order.id().toString()));

        OrderEntity saved = jpaRepository.saveAndFlush(entity);
        entityManager.refresh(saved);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        log.debug("DB lookup order id={}", id);
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        log.debug("DB lookup order idempotencyKey={}", idempotencyKey);
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Page<Order> findAll(Category category, Pageable pageable) {
        log.debug("DB fetch orders category={} page={} size={}", category, pageable.getPageNumber(), pageable.getPageSize());
        return jpaRepository.findAllByCategory(category, pageable).map(mapper::toDomain);
    }

    private OrderEntity updateEntity(OrderEntity existing, Order domain) {
        existing.getOrderItems().clear();

        List<OrderItemEntity> newItems = domain.orderItems().stream()
                .map(item -> OrderItemEntity.builder()
                        .order(existing)
                        .productOfferingId(item.productOfferingId())
                        .quantity(item.quantity())
                        .build())
                .toList();

        existing.getOrderItems().addAll(newItems);
        existing.setState(domain.state());
        existing.setPaymentType(domain.paymentMethod().type());
        existing.setIban(domain.paymentMethod().iban());
        return existing;
    }
}

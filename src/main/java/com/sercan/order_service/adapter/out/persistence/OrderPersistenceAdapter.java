package com.sercan.order_service.adapter.out.persistence;

import com.sercan.order_service.application.port.out.OrderRepository;
import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        log.debug("Saving order id={}", order.id());
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
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
}

package com.sercan.order_service.application.port.out;

import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Page<Order> findAll(Pageable pageable);
}

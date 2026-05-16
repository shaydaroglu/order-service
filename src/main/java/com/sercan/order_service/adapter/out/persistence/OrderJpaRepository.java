package com.sercan.order_service.adapter.out.persistence;

import com.sercan.order_service.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT o FROM OrderEntity o WHERE (:category IS NULL OR o.category = :category)")
    Page<OrderEntity> findAllByCategory(@Param("category") Category category, Pageable pageable);
}

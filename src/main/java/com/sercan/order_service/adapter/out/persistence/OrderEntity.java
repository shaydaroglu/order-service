package com.sercan.order_service.adapter.out.persistence;

import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.OrderItem;
import com.sercan.order_service.domain.OrderState;
import com.sercan.order_service.domain.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private OrderState state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String siteId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod.PaymentType paymentType;

    @Column
    @Setter
    private String iban;

    @Column(unique = true)
    private String idempotencyKey;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}

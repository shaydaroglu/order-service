package com.sercan.order_service.adapter.out.persistence;

import com.sercan.order_service.domain.Order;
import com.sercan.order_service.domain.OrderItem;
import com.sercan.order_service.domain.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    public Order toDomain(OrderEntity entity) {
        List<OrderItem> orderItems = entity.getOrderItems().stream()
                .map(item -> new OrderItem(
                        item.getProductOfferingId(),
                        item.getQuantity()
                ))
                .toList();

        PaymentMethod paymentMethod = new PaymentMethod(
                entity.getPaymentType(),
                entity.getIban()
        );

        return new Order(
                entity.getId(),
                entity.getState(),
                entity.getCategory(),
                entity.getCustomerId(),
                entity.getSiteId(),
                orderItems,
                paymentMethod,
                entity.getIdempotencyKey(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OrderEntity toEntity(Order domain) {
        OrderEntity entity = OrderEntity.builder()
                .id(domain.id())
                .state(domain.state())
                .category(domain.category())
                .customerId(domain.customerId())
                .siteId(domain.siteId())
                .paymentType(domain.paymentMethod().type())
                .iban(domain.paymentMethod().iban())
                .idempotencyKey(domain.idempotencyKey())
                .build();

        List<OrderItemEntity> items = domain.orderItems().stream()
                .map(item -> OrderItemEntity.builder()
                        .order(entity)
                        .productOfferingId(item.productOfferingId())
                        .quantity(item.quantity())
                        .build())
                .toList();

        entity.getOrderItems().addAll(items);
        return entity;
    }
}

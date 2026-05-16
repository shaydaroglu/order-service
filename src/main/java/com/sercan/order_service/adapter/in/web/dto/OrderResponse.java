package com.sercan.order_service.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sercan.order_service.domain.Category;
import com.sercan.order_service.domain.Order;
import com.sercan.order_service.domain.OrderState;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        UUID id,
        OrderState state,
        Category category,
        CustomerDto customer,
        SiteDto site,
        List<OrderItemDto> orderItems,
        PaymentMethodDto paymentMethod,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(),
                order.state(),
                order.category(),
                new CustomerDto(order.customerId()),
                new SiteDto(order.siteId()),
                order.orderItems().stream()
                        .map(item -> new OrderItemDto(item.productOfferingId(), item.quantity()))
                        .toList(),
                new PaymentMethodDto(
                        order.paymentMethod().type(),
                        order.paymentMethod().iban()
                ),
                order.createdAt(),
                order.updatedAt()
        );
    }
}

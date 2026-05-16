package com.sercan.order_service.application.port.in;

import com.sercan.order_service.adapter.in.web.dto.CreateOrderRequest;
import com.sercan.order_service.adapter.in.web.dto.PatchOrderRequest;
import com.sercan.order_service.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderUseCase {
    Order createOrder(CreateOrderRequest order, String idempotencyKey);
    Order getOrder(UUID orderId);
    Page<Order> listOrders(Pageable pageable);
    Order patchOrder(UUID id, PatchOrderRequest request);
}

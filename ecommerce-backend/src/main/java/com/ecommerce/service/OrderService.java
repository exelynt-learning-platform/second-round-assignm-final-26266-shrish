package com.ecommerce.service;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.entity.User;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(User user, OrderRequest request);

    OrderResponse getOrderById(User user, Long orderId);

    List<OrderResponse> getUserOrders(User user);
}

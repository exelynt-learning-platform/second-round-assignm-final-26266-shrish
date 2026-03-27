package com.ecommerce.service;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.User;

public interface PaymentService {

    PaymentResponse processPayment(User user, PaymentRequest request);

    PaymentResponse getPaymentByOrderId(User user, Long orderId);
}

package com.ecommerce.controller;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(user, request);
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(user, orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }
}

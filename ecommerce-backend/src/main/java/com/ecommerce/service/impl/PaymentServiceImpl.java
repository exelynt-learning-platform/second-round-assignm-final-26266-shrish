package com.ecommerce.service.impl;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.User;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.param.ChargeCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal CENTS_PER_DOLLAR = BigDecimal.valueOf(100);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public PaymentResponse processPayment(User user, PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to pay for this order");
        }

        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new BadRequestException("Payment has already been completed for this order");
        }

        String currency = request.getCurrency() != null ? request.getCurrency() : "USD";

        try {
            // Create Stripe charge — convert dollars to cents for Stripe API
            ChargeCreateParams chargeParams = ChargeCreateParams.builder()
                    .setAmount(order.getTotalPrice().multiply(CENTS_PER_DOLLAR).longValue())
                    .setCurrency(currency.toLowerCase())
                    .setSource(request.getToken())
                    .setDescription("Order #" + order.getId() + " payment")
                    .build();

            Charge charge = Charge.create(chargeParams);

            // Save payment record
            Payment payment = Payment.builder()
                    .order(order)
                    .stripePaymentId(charge.getId())
                    .amount(order.getTotalPrice())
                    .currency(currency)
                    .status(PaymentStatus.COMPLETED)
                    .build();

            payment = paymentRepository.save(payment);

            // Update order status
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setOrderStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);

            log.info("Payment successful for order #{}: Stripe charge {}", order.getId(), charge.getId());

            return mapToResponse(payment);

        } catch (StripeException e) {
            log.error("Stripe payment failed for order #{}: {}", order.getId(), e.getMessage());

            // Save failed payment record
            Payment failedPayment = Payment.builder()
                    .order(order)
                    .amount(order.getTotalPrice())
                    .currency(currency)
                    .status(PaymentStatus.FAILED)
                    .build();
            paymentRepository.save(failedPayment);

            // Update order payment status
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            throw new PaymentException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentResponse getPaymentByOrderId(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to view this payment");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .stripePaymentId(payment.getStripePaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

package com.ecommerce.service;

import com.ecommerce.dto.request.PaymentRequest;
import com.ecommerce.dto.response.PaymentResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.User;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("john@example.com")
                .role(Role.USER)
                .build();

        order = Order.builder()
                .id(1L)
                .user(user)
                .totalPrice(new BigDecimal("1999.98"))
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PENDING)
                .build();

        payment = Payment.builder()
                .id(1L)
                .order(order)
                .stripePaymentId("ch_test123")
                .amount(new BigDecimal("1999.98"))
                .currency("USD")
                .status(PaymentStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("Should throw exception when order not found for payment")
    void processPayment_OrderNotFound() {
        PaymentRequest request = PaymentRequest.builder()
                .orderId(999L)
                .token("tok_visa")
                .build();

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(user, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when user is not order owner")
    void processPayment_Unauthorized() {
        User otherUser = User.builder().id(2L).build();
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .token("tok_visa")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(otherUser, request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw exception when payment already completed")
    void processPayment_AlreadyPaid() {
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        PaymentRequest request = PaymentRequest.builder()
                .orderId(1L)
                .token("tok_visa")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been completed");
    }

    @Test
    @DisplayName("Should get payment by order ID")
    void getPaymentByOrderId_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId(user, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getStripePaymentId()).isEqualTo("ch_test123");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void getPaymentByOrderId_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(user, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

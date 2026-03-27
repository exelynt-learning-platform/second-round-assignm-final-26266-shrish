package com.ecommerce.service;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private Cart cart;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role(Role.USER)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(50)
                .build();

        CartItem cartItem = CartItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .cartItems(new ArrayList<>(List.of(cartItem)))
                .totalPrice(new BigDecimal("1999.98"))
                .build();
        cartItem.setCart(cart);

        orderRequest = OrderRequest.builder()
                .shippingAddress("123 Main St, City, Country")
                .build();
    }

    @Test
    @DisplayName("Should create order from cart")
    void createOrder_Success() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        OrderResponse response = orderService.createOrder(user, orderRequest);

        assertThat(response).isNotNull();
        assertThat(response.getShippingAddress()).isEqualTo("123 Main St, City, Country");
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1999.98"));
        assertThat(response.getItems()).hasSize(1);
        verify(cartService).clearCart(user);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when cart is empty")
    void createOrder_EmptyCart() {
        Cart emptyCart = Cart.builder()
                .id(1L)
                .user(user)
                .cartItems(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .build();

        when(cartRepository.findByUser(user)).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> orderService.createOrder(user, orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("Should get order by ID for authorized user")
    void getOrderById_Success() {
        Order order = Order.builder()
                .id(1L)
                .user(user)
                .orderItems(new ArrayList<>())
                .totalPrice(new BigDecimal("1999.98"))
                .shippingAddress("123 Main St")
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(user, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when user tries to view another user's order")
    void getOrderById_Unauthorized() {
        User otherUser = User.builder().id(2L).build();
        Order order = Order.builder()
                .id(1L)
                .user(otherUser)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(user, 1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should throw exception when order not found")
    void getOrderById_NotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(user, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all orders for user")
    void getUserOrders_Success() {
        Order order = Order.builder()
                .id(1L)
                .user(user)
                .orderItems(new ArrayList<>())
                .totalPrice(new BigDecimal("1999.98"))
                .shippingAddress("123 Main St")
                .build();

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(order));

        List<OrderResponse> response = orderService.getUserOrders(user);

        assertThat(response).hasSize(1);
    }
}

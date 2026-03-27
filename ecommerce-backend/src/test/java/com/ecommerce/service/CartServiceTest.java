package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.*;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Product product;
    private Cart cart;

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
                .imageUrl("https://example.com/laptop.jpg")
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .cartItems(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Should get cart for user")
    void getCart_Success() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should add item to cart")
    void addItemToCart_Success() {
        CartItemRequest request = CartItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(user, request);

        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void addItemToCart_InsufficientStock() {
        CartItemRequest request = CartItemRequest.builder()
                .productId(1L)
                .quantity(100)
                .build();

        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItemToCart(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void addItemToCart_ProductNotFound() {
        CartItemRequest request = CartItemRequest.builder()
                .productId(999L)
                .quantity(1)
                .build();

        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItemToCart(user, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should remove item from cart")
    void removeItemFromCart_Success() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(2)
                .unitPrice(product.getPrice())
                .build();
        cart.getCartItems().add(cartItem);

        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.removeItemFromCart(user, 1L);

        assertThat(response).isNotNull();
        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("Should clear cart")
    void clearCart_Success() {
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(user);

        assertThat(cart.getCartItems()).isEmpty();
        assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(cartRepository).save(cart);
    }
}

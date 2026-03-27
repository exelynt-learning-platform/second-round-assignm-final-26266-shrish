package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.User;

public interface CartService {

    CartResponse getCart(User user);

    CartResponse addItemToCart(User user, CartItemRequest request);

    CartResponse updateCartItem(User user, Long cartItemId, Integer quantity);

    CartResponse removeItemFromCart(User user, Long cartItemId);

    void clearCart(User user);
}

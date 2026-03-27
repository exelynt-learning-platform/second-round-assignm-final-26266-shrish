package com.ecommerce.controller;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.entity.User;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal User user) {
        CartResponse response = cartService.getCart(user);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addItemToCart(user, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity) {
        CartResponse response = cartService.updateCartItem(user, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItemFromCart(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId) {
        CartResponse response = cartService.removeItemFromCart(user, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
    }
}

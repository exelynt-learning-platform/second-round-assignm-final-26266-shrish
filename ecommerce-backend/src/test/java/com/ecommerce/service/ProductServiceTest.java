package com.ecommerce.service;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductRequest productRequest;
    private Product product;

    @BeforeEach
    void setUp() {
        productRequest = ProductRequest.builder()
                .name("Laptop")
                .description("High-performance laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(50)
                .imageUrl("https://example.com/laptop.jpg")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("High-performance laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(50)
                .imageUrl("https://example.com/laptop.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(response.getStockQuantity()).isEqualTo(50);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get product by ID")
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("Should throw exception when product not found")
    void getProductById_NotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all products")
    void getAllProducts_Success() {
        Product product2 = Product.builder()
                .id(2L).name("Phone").price(new BigDecimal("599.99")).stockQuantity(100).build();
        when(productRepository.findAll()).thenReturn(Arrays.asList(product, product2));

        List<ProductResponse> response = productService.getAllProducts();

        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct_Success() {
        ProductRequest updateRequest = ProductRequest.builder()
                .name("Updated Laptop")
                .description("Updated description")
                .price(new BigDecimal("1099.99"))
                .stockQuantity(30)
                .imageUrl("https://example.com/updated.jpg")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product successfully")
    void deleteProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("Should search products by name")
    void searchProducts_Success() {
        when(productRepository.findByNameContainingIgnoreCase("Lap"))
                .thenReturn(List.of(product));

        List<ProductResponse> response = productService.searchProducts("Lap");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Laptop");
    }
}

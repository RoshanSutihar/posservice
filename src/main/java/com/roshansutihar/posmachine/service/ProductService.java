package com.roshansutihar.posmachine.service;

import com.roshansutihar.posmachine.entity.Product;
import com.roshansutihar.posmachine.repository.ProductRepository;
import com.roshansutihar.posmachine.request.ProductRequest;
import com.roshansutihar.posmachine.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .upc(request.getUpc())
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .build();

        Product saved = productRepository.save(product);
        return mapToProductResponse(saved);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToProductResponse(product);
    }

    public ProductResponse getProductByUpc(String upc) {
        Product product = productRepository.findByUpc(upc)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToProductResponse(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .upc(product.getUpc())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .build();
    }
}

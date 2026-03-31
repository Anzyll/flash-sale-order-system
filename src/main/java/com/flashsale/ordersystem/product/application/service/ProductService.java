package com.flashsale.ordersystem.product.application.service;

import com.flashsale.ordersystem.product.presentation.dto.CreateProductRequest;
import com.flashsale.ordersystem.product.presentation.dto.ProductResponse;
import com.flashsale.ordersystem.product.application.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.infrastructure.ProductRepository;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    public ProductResponse createProduct(CreateProductRequest request){
        Product product = ProductMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toResponse(savedProduct);
    }
}

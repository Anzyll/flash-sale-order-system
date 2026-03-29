package com.flashsale.ordersystem.product.service;

import com.flashsale.ordersystem.product.dto.CreateProductRequest;
import com.flashsale.ordersystem.product.dto.ProductResponse;
import com.flashsale.ordersystem.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.flashsale.ordersystem.product.entity.Product;
import com.flashsale.ordersystem.product.repository.ProductRepository;

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

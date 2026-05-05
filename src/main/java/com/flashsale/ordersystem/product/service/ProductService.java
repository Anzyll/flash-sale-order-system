package com.flashsale.ordersystem.product.service;

import com.flashsale.ordersystem.shared.exception.BusinessException;
import com.flashsale.ordersystem.shared.exception.ErrorCode;
import com.flashsale.ordersystem.product.presentation.dto.CreateProductRequest;
import com.flashsale.ordersystem.product.presentation.dto.ProductResponse;
import com.flashsale.ordersystem.product.presentation.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    public ProductResponse createProduct(CreateProductRequest request){
        log.info("Creating product. name={}, price={}, stock={}",
                request.name(), request.price(), request.price());
        Product product = ProductMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        log.info("Product created. productId={}", savedProduct.getId());
        return ProductMapper.toResponse(savedProduct);
    }

    public Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}

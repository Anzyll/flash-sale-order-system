package com.flashsale.ordersystem.product.presentation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.flashsale.ordersystem.product.presentation.dto.CreateProductRequest;
import com.flashsale.ordersystem.product.presentation.dto.ProductResponse;
import com.flashsale.ordersystem.product.application.service.ProductService;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {
    private final ProductService productService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody @Valid CreateProductRequest request){
       return productService.createProduct(request);
    }
}

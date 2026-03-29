package com.flashsale.ordersystem.product.mapper;

import com.flashsale.ordersystem.product.dto.CreateProductRequest;
import com.flashsale.ordersystem.product.dto.ProductResponse;
import com.flashsale.ordersystem.product.entity.Product;

public class ProductMapper {
    public static Product toEntity(CreateProductRequest request){
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        return product;
    }
    public static ProductResponse  toResponse(Product product){
        return  new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );
    }
}

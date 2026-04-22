package com.flashsale.ordersystem.sale.application.mapper;

import com.flashsale.ordersystem.product.domain.Product;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.presentation.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.presentation.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;


public class SaleItemMapper {

    public static SaleItem toEntity(
            AddProductToSaleRequest request,
            Sale sale,
            Product product
    ) {
        SaleItem item = new SaleItem();

        item.setSale(sale);
        item.setProduct(product);
        item.setSalePrice(request.salePrice());
        item.setTotalStock(request.totalStock());
        item.setAvailableStock(request.totalStock());

        return item;
    }
    public static SaleItemResponse toResponse(SaleItem item) {
        return new SaleItemResponse(
                item.getId(),
                item.getSale().getId(),
                item.getProduct().getId(),
                item.getSalePrice(),
                item.getAvailableStock()
        );
    }
}
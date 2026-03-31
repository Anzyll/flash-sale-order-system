package com.flashsale.ordersystem.sale.application.mapper;

import com.flashsale.ordersystem.sale.presentation.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.presentation.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.domain.SaleItem;


public class SaleItemMapper {

    public static SaleItem toEntity(AddProductToSaleRequest request, Long saleId) {

        SaleItem item = new SaleItem();
        item.setSaleId(saleId);
        item.setProductId(request.productId());
        item.setSalePrice(request.salePrice());
        item.setTotalStock(request.totalStock());
        item.setAvailableStock(request.totalStock());
        return item;
    }
    public static SaleItemResponse toResponse(SaleItem item) {

        return  new SaleItemResponse(
                item.getId(),
                item.getSaleId(),
                item.getProductId(),
                item.getSalePrice(),
                item.getAvailableStock()
        );

    }
}
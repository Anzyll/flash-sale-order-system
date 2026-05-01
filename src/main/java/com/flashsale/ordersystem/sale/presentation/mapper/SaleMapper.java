package com.flashsale.ordersystem.sale.presentation.mapper;


import com.flashsale.ordersystem.sale.presentation.dto.CreateSaleRequest;
import com.flashsale.ordersystem.sale.presentation.dto.SaleResponse;
import com.flashsale.ordersystem.sale.domain.model.Sale;

public class SaleMapper {
    public static Sale toEntity(CreateSaleRequest request) {
        Sale sale = new Sale();
        sale.setTitle(request.title());
        sale.setStartTime(request.startTime());
        sale.setEndTime(request.endTime());
        return sale;
    }

    public static SaleResponse toResponse(Sale sale) {
       return new SaleResponse(
               sale.getId(),
               sale.getTitle(),
               sale.getStartTime(),
               sale.getEndTime()
      );
    }
}

package com.flashsale.ordersystem.sale.mapper;


import com.flashsale.ordersystem.sale.dto.CreateSaleRequest;
import com.flashsale.ordersystem.sale.dto.SaleResponse;
import com.flashsale.ordersystem.sale.entity.Sale;

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

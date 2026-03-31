package com.flashsale.ordersystem.sale.presentation.controller;
import com.flashsale.ordersystem.sale.application.mapper.SaleItemMapper;
import com.flashsale.ordersystem.sale.application.mapper.SaleMapper;
import com.flashsale.ordersystem.sale.presentation.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.presentation.dto.SaleResponse;
import com.flashsale.ordersystem.sale.application.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {
    private final SaleService saleService;
    @GetMapping
    public List<SaleResponse> getSales(){
        return saleService.getSales()
                .stream()
                .map(SaleMapper::toResponse)
                .toList();
    }
    @GetMapping("/{saleId}/items")
    public List<SaleItemResponse> getSaleItems(@PathVariable Long saleId){
        return saleService.getSaleItems(saleId)
                .stream()
                .map(SaleItemMapper::toResponse)
                .toList();
    }
}

package com.flashsale.ordersystem.sale.controller;
import com.flashsale.ordersystem.sale.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.dto.SaleResponse;
import com.flashsale.ordersystem.sale.service.SaleService;
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
        return saleService.getSales();
    }
    @GetMapping("/{saleId}/items")
    public List<SaleItemResponse> getSaleItems(@PathVariable Long saleId){
        return saleService.getSaleItems(saleId);
    }
}

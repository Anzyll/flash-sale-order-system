package com.flashsale.ordersystem.sale.controller;

import com.flashsale.ordersystem.sale.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.dto.CreateSaleRequest;
import com.flashsale.ordersystem.sale.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.dto.SaleResponse;
import com.flashsale.ordersystem.sale.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sales")
@RequiredArgsConstructor
public class AdminSaleController {
    private final SaleService saleService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse createSale(@RequestBody @Valid CreateSaleRequest request){
        return saleService.createSale(request);
    }
    @PostMapping("/{saleId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleItemResponse addProductToSale(@PathVariable Long saleId,@Valid @RequestBody AddProductToSaleRequest request){
        return saleService.addProductToSale(saleId,request);
    }
}

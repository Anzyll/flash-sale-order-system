package com.flashsale.ordersystem.sale.presentation.controller;

import com.flashsale.ordersystem.sale.presentation.mapper.SaleItemMapper;
import com.flashsale.ordersystem.sale.presentation.mapper.SaleMapper;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import com.flashsale.ordersystem.sale.presentation.dto.AddProductToSaleRequest;
import com.flashsale.ordersystem.sale.presentation.dto.CreateSaleRequest;
import com.flashsale.ordersystem.sale.presentation.dto.SaleItemResponse;
import com.flashsale.ordersystem.sale.presentation.dto.SaleResponse;
import com.flashsale.ordersystem.sale.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/sales")
@RequiredArgsConstructor
public class AdminSaleController {
    private final SaleService saleService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse createSale(@RequestBody @Valid CreateSaleRequest request){
        Sale sale = saleService.createSale(request);
        return SaleMapper.toResponse(sale);
    }
    @PostMapping("/{saleId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public SaleItemResponse addProductToSale(@PathVariable Long saleId,@Valid @RequestBody AddProductToSaleRequest request){
        SaleItem item = saleService.addProductToSale(saleId,request);
        return SaleItemMapper.toResponse(item);
    }
}

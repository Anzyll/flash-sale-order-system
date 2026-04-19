package com.flashsale.ordersystem.order.presentation.controller;


import com.flashsale.ordersystem.order.application.service.PurchaseService;
import com.flashsale.ordersystem.order.presentation.dto.PurchaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales/{saleId}/purchase")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String purchase(@PathVariable Long saleId, @Valid @RequestBody PurchaseRequest request){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getSubject();
        String correlationId = UUID.randomUUID().toString();
        purchaseService.purchase(userId,saleId,request.productId(),correlationId);
        return "Order is being processed";
    }
}

package com.flashsale.ordersystem.order.presentation.controller;


import com.flashsale.ordersystem.order.application.mapper.PurchaseMapper;
import com.flashsale.ordersystem.order.application.service.PurchaseService;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.presentation.dto.PurchaseRequest;
import com.flashsale.ordersystem.order.presentation.dto.PurchaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/v1/sales/{saleId}/purchase")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse purchase(@PathVariable Long saleId, @Valid @RequestBody PurchaseRequest request){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getSubject();
        Order order = purchaseService.purchase(userId,saleId,request.productId());
        return PurchaseMapper.toResponse(order);

    }
}

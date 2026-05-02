package com.flashsale.ordersystem.order.adapter.rest.controller;


import com.flashsale.ordersystem.order.service.OrderService;
import com.flashsale.ordersystem.order.adapter.rest.dto.PurchaseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sales/{saleId}/purchase")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Slf4j
public class PurchaseController {
    private final OrderService orderService;
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String purchase(@PathVariable Long saleId, @Valid @RequestBody PurchaseRequest request){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = jwt.getSubject();
        log.info("Incoming purchase request. userId={}, saleId={}, productId={}",
                userId, saleId, request.productId());
        orderService.purchase(userId,saleId,request.productId());
        return "Order is being processed";
    }
}

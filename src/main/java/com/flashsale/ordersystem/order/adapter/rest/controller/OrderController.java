package com.flashsale.ordersystem.order.adapter.rest.controller;

import com.flashsale.ordersystem.order.adapter.rest.dto.OrderStatusResponse;
import com.flashsale.ordersystem.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/orders/my-orders")
@PreAuthorize("hasRole('USER')")
public class OrderController {
    private final OrderService orderService;
    @GetMapping("/{orderId}")
    public OrderStatusResponse getOrderStatus(@PathVariable Long orderId){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        String userId = jwt.getSubject();
      return  orderService.getOrderStatus(userId,orderId);
    }
}

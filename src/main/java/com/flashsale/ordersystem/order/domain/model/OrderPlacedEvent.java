package com.flashsale.ordersystem.order.domain.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OrderPlacedEvent {
    private String eventId;
    private String userId;
    private Long saleId;
    private Long productId;
    private Long timestamp;
}
package com.flashsale.ordersystem.order.scheduler;

import com.flashsale.ordersystem.shared.port.StockReservationPort;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.adapter.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderExpiryScheduler {
    private final OrderRepository orderRepository;
    private final StockReservationPort stockReservationPort;
    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void expirePendingOrders(){
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<Order> orders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,threshold);
        for(Order order : orders){
            int updated = orderRepository.expireIfPending(order.getId(),OrderStatus.PENDING,OrderStatus.EXPIRED);
            if (updated==0) {
                continue;
            }
            stockReservationPort.revertPurchase(
                    order.getUser().getKeycloakId(),
                    order.getSale().getId(),
                    order.getProduct().getId(),
                    1
            );
            log.warn("Order expired and compensated. orderId={}", order.getId());
        }
    }
}

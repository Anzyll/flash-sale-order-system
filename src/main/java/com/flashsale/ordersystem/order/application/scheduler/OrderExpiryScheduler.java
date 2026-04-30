package com.flashsale.ordersystem.order.application.scheduler;

import com.flashsale.ordersystem.order.application.port.StockService;
import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import com.flashsale.ordersystem.order.infrastructure.repository.OrderRepository;
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
    private final StockService stockService;
    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void expirePendingOrders(){
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);
        List<Order> orders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,threshold);

        for(Order order : orders){
            int updated = orderRepository.expireIfPending(order.getId());
            if (updated==0) {
                continue;
            }
            stockService.revertPurchase(
                    order.getUser().getKeycloakId(),
                    order.getSale().getId(),
                    order.getProduct().getId(),
                    1
            );
            log.warn("Order expired and compensated. orderId={}", order.getId());
        }
    }
}

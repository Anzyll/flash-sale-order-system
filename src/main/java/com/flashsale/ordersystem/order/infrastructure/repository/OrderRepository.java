package com.flashsale.ordersystem.order.infrastructure.repository;

import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {
    @Query("""
    SELECT COALESCE(SUM(oi.quantity), 0)
    FROM OrderItem oi
    WHERE oi.order.sale.id = :saleId
      AND oi.product.id = :productId
      AND oi.order.status = :status
""")
    long countSoldQuantity(Long saleId, Long productId, OrderStatus status);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime threshold);

    @Modifying
    @Query("""
    UPDATE Order o 
    SET o.status='EXPIRED' 
    WHERE o.id = :id  AND o.status = 'PENDING'
    """)
    int expireIfPending(Long id);
}

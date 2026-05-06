package com.flashsale.ordersystem.order.adapter.persistence;

import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant threshold);

    @Modifying
    @Query("""
    UPDATE Order o 
    SET o.status= :expiredStatus
    WHERE o.id = :id  AND o.status = :pendingStatus
    """)
    int expireIfPending(@Param("id") Long id, @Param("pendingStatus") OrderStatus pendingStatus,@Param("expiredStatus") OrderStatus expiredStatus);
}

package com.flashsale.ordersystem.order.infrastructure.repository;

import com.flashsale.ordersystem.order.domain.enums.OrderStatus;
import com.flashsale.ordersystem.order.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}

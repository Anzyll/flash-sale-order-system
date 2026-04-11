package com.flashsale.ordersystem.sale.infrastructure;

import com.flashsale.ordersystem.sale.domain.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem,Long> {
    List<SaleItem> findAllBySaleId(Long saleId);
    Optional<SaleItem> findBySaleIdAndProductId(Long saleId,Long productId);
}

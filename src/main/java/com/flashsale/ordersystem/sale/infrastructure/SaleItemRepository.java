package com.flashsale.ordersystem.sale.infrastructure;

import com.flashsale.ordersystem.sale.domain.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem,Long> {
    List<SaleItem> findAllBySaleId(Long saleId);
    Optional<SaleItem> findBySaleIdAndProductId(Long saleId,Long productId);

}

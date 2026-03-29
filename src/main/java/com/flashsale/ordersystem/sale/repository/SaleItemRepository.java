package com.flashsale.ordersystem.sale.repository;

import com.flashsale.ordersystem.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem,Long> {
    List<SaleItem> findAllBySaleId(Long saleId);
}

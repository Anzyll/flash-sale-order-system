package com.flashsale.ordersystem.sale.infrastructure;

import com.flashsale.ordersystem.sale.domain.enums.SaleStatus;
import com.flashsale.ordersystem.sale.domain.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale,Long> {
    List<Sale> findByStartTimeBeforeAndStatus(LocalDateTime startTimeBefore, SaleStatus status);
    List<Sale> findByEndTimeBeforeAndStatus(LocalDateTime endTimeBefore, SaleStatus status);

}

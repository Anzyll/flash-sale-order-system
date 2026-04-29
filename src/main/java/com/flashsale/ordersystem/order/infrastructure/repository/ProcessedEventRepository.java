package com.flashsale.ordersystem.order.infrastructure.repository;

import com.flashsale.ordersystem.order.domain.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}

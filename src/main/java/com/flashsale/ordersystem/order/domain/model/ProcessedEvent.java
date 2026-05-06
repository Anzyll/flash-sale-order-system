package com.flashsale.ordersystem.order.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;


import java.time.Instant;

@Entity
@NoArgsConstructor
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;
    private Instant processedAt = Instant.now();

    public ProcessedEvent(String eventId) {
        this.eventId=eventId;
    }
}

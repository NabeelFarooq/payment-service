package com.ecommerce.payment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Id
    @Column(nullable = false, length = 80)
    private String eventId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ProcessedEventEntity() {
    }

    public ProcessedEventEntity(String eventId, LocalDateTime processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
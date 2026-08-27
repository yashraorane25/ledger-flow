package com.ledgerflow.payment_service.event;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

// Maps to the same processed_events table ledger-service's V1__ledger.sql
// creates. Both services share one physical Postgres schema in this project,
// and the table's composite key (event_id, consumer_group) is designed to
// hold rows for multiple consumer groups side by side, so payment-service
// reuses it rather than creating a second, differently-named copy.
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @EmbeddedId
    private ProcessedEventId id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(ProcessedEventId id) {
        this.id = id;
    }

    @PrePersist
    void onCreate() {
        this.processedAt = Instant.now();
    }

    public ProcessedEventId getId() {
        return id;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}

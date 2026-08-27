package com.ledgerflow.ledger_service.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letters")
public class DeadLetter {

    @Id
    private UUID id;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "event_payload")
    private String eventPayload;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    protected DeadLetter() {
    }

    public DeadLetter(UUID id, String originalTopic, String eventPayload, String errorMessage) {
        this.id = id;
        this.originalTopic = originalTopic;
        this.eventPayload = eventPayload;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void onCreate() {
        this.failedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public String getEventPayload() {
        return eventPayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getFailedAt() {
        return failedAt;
    }
}

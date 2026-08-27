package com.ledgerflow.ledger_service.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// Read-only mirror of payment-service's own `payments` table. Both services
// share one physical Postgres schema in this project (a deliberate demo
// simplification, same reasoning as reusing processed_events), so
// reconciliation reads it directly instead of calling payment-service over
// HTTP. `payments` is payment-service's table to own — nothing here writes
// to it.
@Entity
@Table(name = "payments")
public class PaymentSnapshot {

    @Id
    private UUID id;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentSnapshot() {
    }

    public UUID getId() {
        return id;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

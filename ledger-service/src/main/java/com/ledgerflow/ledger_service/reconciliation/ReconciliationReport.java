package com.ledgerflow.ledger_service.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_reports")
public class ReconciliationReport {

    @Id
    private UUID id;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "mismatch_type", nullable = false, length = 50)
    private String mismatchType;

    @Column(name = "expected_minor")
    private Long expectedMinor;

    @Column(name = "actual_minor")
    private Long actualMinor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReconciliationReport() {
    }

    public ReconciliationReport(UUID id, LocalDate runDate, UUID paymentId, String mismatchType,
                                 Long expectedMinor, Long actualMinor) {
        this.id = id;
        this.runDate = runDate;
        this.paymentId = paymentId;
        this.mismatchType = mismatchType;
        this.expectedMinor = expectedMinor;
        this.actualMinor = actualMinor;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getMismatchType() {
        return mismatchType;
    }

    public Long getExpectedMinor() {
        return expectedMinor;
    }

    public Long getActualMinor() {
        return actualMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.ledgerflow.ledger_service.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentSnapshotRepository extends JpaRepository<PaymentSnapshot, UUID> {
}

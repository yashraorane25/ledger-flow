package com.ledgerflow.ledger_service.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JournalRepository extends JpaRepository<Journal, UUID> {

    List<Journal> findByPaymentId(UUID paymentId);
}

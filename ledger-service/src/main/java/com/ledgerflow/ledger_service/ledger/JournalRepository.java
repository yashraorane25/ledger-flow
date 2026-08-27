package com.ledgerflow.ledger_service.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalRepository extends JpaRepository<Journal, UUID> {
}

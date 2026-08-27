package com.ledgerflow.ledger_service.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeadLetterRepository extends JpaRepository<DeadLetter, UUID> {
}

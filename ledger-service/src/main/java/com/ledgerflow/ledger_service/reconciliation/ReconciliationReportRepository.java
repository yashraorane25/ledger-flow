package com.ledgerflow.ledger_service.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, UUID> {

    List<ReconciliationReport> findTop100ByOrderByCreatedAtDesc();
}

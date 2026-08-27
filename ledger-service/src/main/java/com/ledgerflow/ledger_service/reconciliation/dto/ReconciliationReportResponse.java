package com.ledgerflow.ledger_service.reconciliation.dto;

import com.ledgerflow.ledger_service.reconciliation.ReconciliationReport;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReconciliationReportResponse(
        UUID id,
        LocalDate runDate,
        UUID paymentId,
        String mismatchType,
        Long expectedMinor,
        Long actualMinor,
        Instant createdAt
) {
    public static ReconciliationReportResponse from(ReconciliationReport report) {
        return new ReconciliationReportResponse(report.getId(), report.getRunDate(), report.getPaymentId(),
                report.getMismatchType(), report.getExpectedMinor(), report.getActualMinor(), report.getCreatedAt());
    }
}

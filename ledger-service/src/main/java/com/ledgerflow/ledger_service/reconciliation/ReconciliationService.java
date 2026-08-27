package com.ledgerflow.ledger_service.reconciliation;

import com.ledgerflow.ledger_service.ledger.EntryDirection;
import com.ledgerflow.ledger_service.ledger.Journal;
import com.ledgerflow.ledger_service.ledger.JournalRepository;
import com.ledgerflow.ledger_service.ledger.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Cross-checks payment-service's source-of-truth payments against the
// journals/ledger_entries that ledger-service's own Kafka consumer booked
// for them. Catches drift such as a payment whose PaymentCreated event
// never got journaled (e.g. it was dead-lettered — see DeadLetterHandler).
@Service
public class ReconciliationService {

    // Skip payments younger than this. The outbox relay + Kafka round trip
    // takes a moment, so a payment created a second ago may legitimately not
    // have a journal yet — that's still in flight, not a real mismatch.
    private static final long GRACE_PERIOD_SECONDS = 10;

    private static final String MISSING_JOURNAL = "MISSING_JOURNAL";
    private static final String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final PaymentSnapshotRepository paymentSnapshotRepository;
    private final JournalRepository journalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReconciliationReportRepository reconciliationReportRepository;

    public ReconciliationService(PaymentSnapshotRepository paymentSnapshotRepository,
                                  JournalRepository journalRepository,
                                  LedgerEntryRepository ledgerEntryRepository,
                                  ReconciliationReportRepository reconciliationReportRepository) {
        this.paymentSnapshotRepository = paymentSnapshotRepository;
        this.journalRepository = journalRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.reconciliationReportRepository = reconciliationReportRepository;
    }

    @Transactional
    public List<ReconciliationReport> run() {
        LocalDate runDate = LocalDate.now();
        Instant cutoff = Instant.now().minus(GRACE_PERIOD_SECONDS, ChronoUnit.SECONDS);
        List<ReconciliationReport> mismatches = new ArrayList<>();

        for (PaymentSnapshot payment : paymentSnapshotRepository.findAll()) {
            if (payment.getCreatedAt().isAfter(cutoff)) {
                continue;
            }

            List<Journal> journals = journalRepository.findByPaymentId(payment.getId());
            if (journals.isEmpty()) {
                mismatches.add(report(runDate, payment.getId(), MISSING_JOURNAL, payment.getAmountMinor(), null));
                continue;
            }

            long booked = ledgerEntryRepository.sumByJournalIdAndDirection(journals.get(0).getId(), EntryDirection.DR);
            if (booked != payment.getAmountMinor()) {
                mismatches.add(report(runDate, payment.getId(), AMOUNT_MISMATCH, payment.getAmountMinor(), booked));
            }
        }

        reconciliationReportRepository.saveAll(mismatches);
        if (!mismatches.isEmpty()) {
            log.warn("Reconciliation found {} mismatch(es)", mismatches.size());
        }
        return mismatches;
    }

    private ReconciliationReport report(LocalDate runDate, UUID paymentId, String mismatchType,
                                         Long expectedMinor, Long actualMinor) {
        return new ReconciliationReport(UUID.randomUUID(), runDate, paymentId, mismatchType, expectedMinor, actualMinor);
    }
}

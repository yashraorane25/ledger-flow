package com.ledgerflow.ledger_service.reconciliation;

import com.ledgerflow.ledger_service.ledger.EntryDirection;
import com.ledgerflow.ledger_service.ledger.Journal;
import com.ledgerflow.ledger_service.ledger.JournalRepository;
import com.ledgerflow.ledger_service.ledger.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private PaymentSnapshotRepository paymentSnapshotRepository;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private ReconciliationReportRepository reconciliationReportRepository;

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService(paymentSnapshotRepository, journalRepository,
                ledgerEntryRepository, reconciliationReportRepository);
    }

    // PaymentSnapshot is a read-only JPA mapping onto payment-service's own
    // table with no public constructor by design (see its class comment), so
    // tests mock it directly rather than constructing real instances.
    private PaymentSnapshot snapshotAgedSeconds(long amountMinor, long ageSeconds) {
        PaymentSnapshot snapshot = mock(PaymentSnapshot.class);
        // getId()/getAmountMinor() aren't reached for payments skipped on age
        // (see recentPaymentIsSkippedRegardlessOfJournalState), so lenient
        // avoids a false "unnecessary stubbing" failure in that test.
        lenient().when(snapshot.getId()).thenReturn(UUID.randomUUID());
        lenient().when(snapshot.getAmountMinor()).thenReturn(amountMinor);
        when(snapshot.getCreatedAt()).thenReturn(Instant.now().minusSeconds(ageSeconds));
        return snapshot;
    }

    @Test
    void paymentWithNoJournalIsFlaggedMissing() {
        PaymentSnapshot payment = snapshotAgedSeconds(1000, 60);
        when(paymentSnapshotRepository.findAll()).thenReturn(List.of(payment));
        when(journalRepository.findByPaymentId(payment.getId())).thenReturn(List.of());

        List<ReconciliationReport> mismatches = reconciliationService.run();

        assertEquals(1, mismatches.size());
        assertEquals("MISSING_JOURNAL", mismatches.get(0).getMismatchType());
        assertEquals(1000L, mismatches.get(0).getExpectedMinor());
    }

    @Test
    void paymentWithMatchingJournalIsNotFlagged() {
        PaymentSnapshot payment = snapshotAgedSeconds(1000, 60);
        Journal journal = new Journal(UUID.randomUUID(), payment.getId(), "PaymentCreated", "Payment created");
        when(paymentSnapshotRepository.findAll()).thenReturn(List.of(payment));
        when(journalRepository.findByPaymentId(payment.getId())).thenReturn(List.of(journal));
        when(ledgerEntryRepository.sumByJournalIdAndDirection(journal.getId(), EntryDirection.DR)).thenReturn(1000L);

        List<ReconciliationReport> mismatches = reconciliationService.run();

        assertTrue(mismatches.isEmpty());
    }

    @Test
    void bookedAmountMismatchIsFlagged() {
        PaymentSnapshot payment = snapshotAgedSeconds(1000, 60);
        Journal journal = new Journal(UUID.randomUUID(), payment.getId(), "PaymentCreated", "Payment created");
        when(paymentSnapshotRepository.findAll()).thenReturn(List.of(payment));
        when(journalRepository.findByPaymentId(payment.getId())).thenReturn(List.of(journal));
        when(ledgerEntryRepository.sumByJournalIdAndDirection(journal.getId(), EntryDirection.DR)).thenReturn(700L);

        List<ReconciliationReport> mismatches = reconciliationService.run();

        assertEquals(1, mismatches.size());
        assertEquals("AMOUNT_MISMATCH", mismatches.get(0).getMismatchType());
        assertEquals(700L, mismatches.get(0).getActualMinor());
    }

    @Test
    void recentPaymentIsSkippedRegardlessOfJournalState() {
        PaymentSnapshot payment = snapshotAgedSeconds(1000, 2);
        when(paymentSnapshotRepository.findAll()).thenReturn(List.of(payment));

        List<ReconciliationReport> mismatches = reconciliationService.run();

        assertTrue(mismatches.isEmpty());
    }
}

package com.ledgerflow.ledger_service.event;

import com.ledgerflow.ledger_service.ledger.BalanceService;
import com.ledgerflow.ledger_service.ledger.EntryDirection;
import com.ledgerflow.ledger_service.ledger.Journal;
import com.ledgerflow.ledger_service.ledger.JournalRepository;
import com.ledgerflow.ledger_service.ledger.LedgerAccount;
import com.ledgerflow.ledger_service.ledger.LedgerAccountRepository;
import com.ledgerflow.ledger_service.ledger.LedgerEntry;
import com.ledgerflow.ledger_service.ledger.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventProcessorTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;
    @Mock
    private LedgerAccountRepository ledgerAccountRepository;
    @Mock
    private BalanceService balanceService;

    private PaymentEventProcessor processor;

    private final LedgerAccount pspReceivable =
            new LedgerAccount(UUID.randomUUID(), "PSP_RECEIVABLE", "ASSET", "USD");
    private final LedgerAccount merchantPayable =
            new LedgerAccount(UUID.randomUUID(), "MERCHANT_PAYABLE", "LIABILITY", "USD");

    @BeforeEach
    void setUp() {
        processor = new PaymentEventProcessor(processedEventRepository, journalRepository,
                ledgerEntryRepository, ledgerAccountRepository, balanceService);
    }

    // Directly asserts the rule CLAUDE.md calls out: journal debits must equal credits.
    @Test
    void bookingIsBalancedAndBothAccountsEvicted() {
        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(ledgerAccountRepository.findByAccountCode("PSP_RECEIVABLE")).thenReturn(Optional.of(pspReceivable));
        when(ledgerAccountRepository.findByAccountCode("MERCHANT_PAYABLE")).thenReturn(Optional.of(merchantPayable));

        PaymentEventEnvelope event = new PaymentEventEnvelope(
                UUID.randomUUID(), "PaymentCreated", UUID.randomUUID(), "idem-1", 4200, "USD", "PENDING", null);

        processor.handle(event);

        ArgumentCaptor<LedgerEntry> entryCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(entryCaptor.capture());

        List<LedgerEntry> entries = entryCaptor.getAllValues();
        long debitTotal = entries.stream().filter(e -> e.getDirection() == EntryDirection.DR)
                .mapToLong(LedgerEntry::getAmountMinor).sum();
        long creditTotal = entries.stream().filter(e -> e.getDirection() == EntryDirection.CR)
                .mapToLong(LedgerEntry::getAmountMinor).sum();

        assertEquals(4200, debitTotal);
        assertEquals(debitTotal, creditTotal, "debits must equal credits");

        verify(journalRepository, times(1)).save(any(Journal.class));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(balanceService).evict("PSP_RECEIVABLE");
        verify(balanceService).evict("MERCHANT_PAYABLE");
    }

    @Test
    void alreadyProcessedEventIsSkipped() {
        when(processedEventRepository.existsById(any())).thenReturn(true);

        PaymentEventEnvelope event = new PaymentEventEnvelope(
                UUID.randomUUID(), "PaymentCreated", UUID.randomUUID(), "idem-2", 100, "USD", "PENDING", null);

        processor.handle(event);

        verify(journalRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    void unhandledEventTypeIsRecordedButNotBooked() {
        when(processedEventRepository.existsById(any())).thenReturn(false);

        PaymentEventEnvelope event = new PaymentEventEnvelope(
                UUID.randomUUID(), "PaymentAuthorized", UUID.randomUUID(), "idem-3", 100, "USD", "AUTHORIZED", null);

        processor.handle(event);

        verify(journalRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }
}

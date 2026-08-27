package com.ledgerflow.ledger_service.event;

import com.ledgerflow.ledger_service.ledger.BalanceService;
import com.ledgerflow.ledger_service.ledger.EntryDirection;
import com.ledgerflow.ledger_service.ledger.Journal;
import com.ledgerflow.ledger_service.ledger.JournalRepository;
import com.ledgerflow.ledger_service.ledger.LedgerAccount;
import com.ledgerflow.ledger_service.ledger.LedgerAccountRepository;
import com.ledgerflow.ledger_service.ledger.LedgerEntry;
import com.ledgerflow.ledger_service.ledger.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentEventProcessor {

    static final String CONSUMER_GROUP = "ledger-service";
    private static final String PSP_RECEIVABLE = "PSP_RECEIVABLE";
    private static final String MERCHANT_PAYABLE = "MERCHANT_PAYABLE";

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProcessor.class);

    private final ProcessedEventRepository processedEventRepository;
    private final JournalRepository journalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final BalanceService balanceService;

    public PaymentEventProcessor(ProcessedEventRepository processedEventRepository,
                                  JournalRepository journalRepository,
                                  LedgerEntryRepository ledgerEntryRepository,
                                  LedgerAccountRepository ledgerAccountRepository,
                                  BalanceService balanceService) {
        this.processedEventRepository = processedEventRepository;
        this.journalRepository = journalRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.balanceService = balanceService;
    }

    @Transactional
    public void handle(PaymentEventEnvelope event) {
        ProcessedEventId processedEventId = new ProcessedEventId(event.eventId(), CONSUMER_GROUP);
        if (processedEventRepository.existsById(processedEventId)) {
            log.info("Event {} already processed by {}, skipping", event.eventId(), CONSUMER_GROUP);
            return;
        }

        if (!"PaymentCreated".equals(event.eventType())) {
            log.debug("Ignoring unhandled event type {}", event.eventType());
            processedEventRepository.save(new ProcessedEvent(processedEventId));
            return;
        }

        bookPaymentCreated(event);
        processedEventRepository.save(new ProcessedEvent(processedEventId));
    }

    private void bookPaymentCreated(PaymentEventEnvelope event) {
        LedgerAccount pspReceivable = requireAccount(PSP_RECEIVABLE);
        LedgerAccount merchantPayable = requireAccount(MERCHANT_PAYABLE);

        Journal journal = new Journal(UUID.randomUUID(), event.paymentId(), event.eventType(), "Payment created");
        journalRepository.save(journal);

        LedgerEntry debit = new LedgerEntry(UUID.randomUUID(), journal.getId(), pspReceivable.getId(),
                EntryDirection.DR, event.amountMinor(), event.currency());
        LedgerEntry credit = new LedgerEntry(UUID.randomUUID(), journal.getId(), merchantPayable.getId(),
                EntryDirection.CR, event.amountMinor(), event.currency());

        if (debit.getAmountMinor() != credit.getAmountMinor()) {
            throw new IllegalStateException("Journal " + journal.getId() + " is not balanced: debits != credits");
        }

        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        balanceService.evict(PSP_RECEIVABLE);
        balanceService.evict(MERCHANT_PAYABLE);
    }

    private LedgerAccount requireAccount(String accountCode) {
        return ledgerAccountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new IllegalStateException("Missing seeded ledger account: " + accountCode));
    }
}

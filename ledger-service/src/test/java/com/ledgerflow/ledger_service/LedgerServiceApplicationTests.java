package com.ledgerflow.ledger_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.ledger_service.ledger.EntryDirection;
import com.ledgerflow.ledger_service.ledger.Journal;
import com.ledgerflow.ledger_service.ledger.JournalRepository;
import com.ledgerflow.ledger_service.ledger.LedgerEntry;
import com.ledgerflow.ledger_service.ledger.LedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LedgerServiceApplicationTests {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JournalRepository journalRepository;

	@Autowired
	private LedgerEntryRepository ledgerEntryRepository;

	@Test
	void contextLoads() {
	}

	// Publishes a real message onto payments.events (the same topic the
	// outbox relay publishes to) and waits for PaymentEventsConsumer to
	// actually book it, proving the Kafka -> Postgres path works end-to-end
	// against the real Testcontainers-backed broker/database this class spins up.
	@Test
	void paymentCreatedEventGetsBookedAsBalancedJournal() throws Exception {
		UUID paymentId = UUID.randomUUID();
		Map<String, Object> payload = Map.of(
				"eventId", UUID.randomUUID().toString(),
				"eventType", "PaymentCreated",
				"paymentId", paymentId.toString(),
				"idempotencyKey", "ledger-flow-test-" + paymentId,
				"amountMinor", 4200,
				"currency", "USD",
				"status", "PENDING"
		);

		kafkaTemplate.send("payments.events", paymentId.toString(), objectMapper.writeValueAsString(payload)).get();

		Journal journal = pollForJournal(paymentId);

		List<LedgerEntry> entries = ledgerEntryRepository.findByJournalId(journal.getId());
		assertEquals(2, entries.size());

		long debitTotal = entries.stream().filter(e -> e.getDirection() == EntryDirection.DR)
				.mapToLong(LedgerEntry::getAmountMinor).sum();
		long creditTotal = entries.stream().filter(e -> e.getDirection() == EntryDirection.CR)
				.mapToLong(LedgerEntry::getAmountMinor).sum();

		assertEquals(4200, debitTotal);
		assertEquals(debitTotal, creditTotal, "debits must equal credits");
	}

	private Journal pollForJournal(UUID paymentId) throws InterruptedException {
		for (int attempt = 0; attempt < 20; attempt++) {
			List<Journal> journals = journalRepository.findByPaymentId(paymentId);
			if (!journals.isEmpty()) {
				return journals.get(0);
			}
			Thread.sleep(500);
		}
		fail("No journal booked for payment " + paymentId + " within timeout");
		return null;
	}
}

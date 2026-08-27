package com.ledgerflow.payment_service;

import com.ledgerflow.payment_service.payment.dto.CreatePaymentRequest;
import com.ledgerflow.payment_service.payment.dto.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class PaymentServiceApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
	}

	// Exercises the real pipeline: HTTP -> outbox -> Kafka -> AuthorizationConsumer
	// -> status update, against the real Postgres/Kafka/Redis Testcontainers spin
	// up for this class. Proves the whole thing actually wires together, not just
	// that the individual classes behave correctly in isolation.
	@Test
	void smallPaymentEventuallyAuthorizes() {
		UUID paymentId = createPayment(5_000, "auth-flow-" + UUID.randomUUID());
		assertEquals("AUTHORIZED", pollUntilFinal(paymentId));
	}

	@Test
	void largePaymentEventuallyDeclines() {
		UUID paymentId = createPayment(2_000_000, "auth-flow-" + UUID.randomUUID());
		assertEquals("FAILED", pollUntilFinal(paymentId));
	}

	private UUID createPayment(long amountMinor, String idempotencyKey) {
		CreatePaymentRequest request = new CreatePaymentRequest(idempotencyKey, amountMinor, "USD");
		ResponseEntity<PaymentResponse> response = restTemplate.postForEntity("/payments", request, PaymentResponse.class);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		PaymentResponse body = response.getBody();
		assertEquals("PENDING", body.status());
		return body.id();
	}

	// The outbox relay polls every ~1s; give the round trip through Kafka and
	// back to AuthorizationConsumer plenty of headroom before giving up.
	private String pollUntilFinal(UUID paymentId) {
		for (int attempt = 0; attempt < 20; attempt++) {
			ResponseEntity<PaymentResponse> response = restTemplate.getForEntity("/payments/" + paymentId, PaymentResponse.class);
			String status = response.getBody().status();
			if (!"PENDING".equals(status)) {
				return status;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail("Interrupted while polling payment status");
			}
		}
		fail("Payment " + paymentId + " never left PENDING status within timeout");
		return null;
	}
}

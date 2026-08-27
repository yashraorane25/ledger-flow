package com.ledgerflow.payment_service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.payment_service.authorization.AuthorizationSimulator;
import com.ledgerflow.payment_service.outbox.OutboxEvent;
import com.ledgerflow.payment_service.outbox.OutboxEventRepository;
import com.ledgerflow.payment_service.payment.dto.CreatePaymentRequest;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        AuthorizationSimulator authorizationSimulator = new AuthorizationSimulator(1_000_000L);
        paymentService = new PaymentService(paymentRepository, outboxEventRepository, authorizationSimulator, new ObjectMapper());
    }

    @Test
    void duplicateKeyReturnsSamePayment() {
        Payment existing = new Payment(UUID.randomUUID(), "dup-key", 500, "USD", PaymentStatus.PENDING);
        when(paymentRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        Payment result = paymentService.createPayment(new CreatePaymentRequest("dup-key", 500, "USD"));

        assertSame(existing, result);
        verify(paymentRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void newPaymentWritesBothOutboxEvents() throws Exception {
        when(paymentRepository.findByIdempotencyKey("new-key")).thenReturn(Optional.empty());

        Payment result = paymentService.createPayment(new CreatePaymentRequest("new-key", 5_000, "USD"));

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentRepository, times(1)).save(result);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(outboxCaptor.capture());

        List<OutboxEvent> events = outboxCaptor.getAllValues();
        assertEquals("PaymentCreated", events.get(0).getEventType());
        assertEquals("PaymentAuthorized", events.get(1).getEventType());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode authPayload = objectMapper.readTree(events.get(1).getPayload());
        assertEquals("AUTHORIZED", authPayload.get("status").asText());
    }

    @Test
    void largeAmountIsDeclined() throws Exception {
        when(paymentRepository.findByIdempotencyKey("big-key")).thenReturn(Optional.empty());

        paymentService.createPayment(new CreatePaymentRequest("big-key", 2_000_000, "USD"));

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(outboxCaptor.capture());

        OutboxEvent authEvent = outboxCaptor.getAllValues().get(1);
        assertEquals("PaymentAuthorizationFailed", authEvent.getEventType());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode payload = objectMapper.readTree(authEvent.getPayload());
        assertEquals("FAILED", payload.get("status").asText());
        assertEquals("AMOUNT_EXCEEDS_LIMIT", payload.get("failureReason").asText());
    }
}

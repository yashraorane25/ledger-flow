package com.ledgerflow.payment_service.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.payment_service.authorization.AuthorizationDecision;
import com.ledgerflow.payment_service.authorization.AuthorizationSimulator;
import com.ledgerflow.payment_service.outbox.OutboxEvent;
import com.ledgerflow.payment_service.outbox.OutboxEventRepository;
import com.ledgerflow.payment_service.payment.dto.CreatePaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AuthorizationSimulator authorizationSimulator;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                           OutboxEventRepository outboxEventRepository,
                           AuthorizationSimulator authorizationSimulator,
                           ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.authorizationSimulator = authorizationSimulator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {
        return paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseGet(() -> createNewPayment(request));
    }

    private Payment createNewPayment(CreatePaymentRequest request) {
        Payment payment = new Payment(
                UUID.randomUUID(),
                request.idempotencyKey(),
                request.amountMinor(),
                request.currency(),
                PaymentStatus.PENDING
        );
        paymentRepository.save(payment);

        UUID eventId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "eventId", eventId.toString(),
                "eventType", "PaymentCreated",
                "paymentId", payment.getId().toString(),
                "idempotencyKey", payment.getIdempotencyKey(),
                "amountMinor", payment.getAmountMinor(),
                "currency", payment.getCurrency(),
                "status", payment.getStatus().name()
        );
        OutboxEvent event = new OutboxEvent(
                eventId,
                payment.getId(),
                "PAYMENT",
                "PaymentCreated",
                writeJson(payload)
        );
        outboxEventRepository.save(event);

        publishAuthorizationOutcome(payment);

        return payment;
    }

    // Simulates an async authorization callback: writes a second outbox event
    // in the same transaction as payment creation, which AuthorizationConsumer
    // later picks up (via Kafka, after the relay publishes it) to flip the
    // payment's status. See AuthorizationSimulator for the decision rule.
    private void publishAuthorizationOutcome(Payment payment) {
        AuthorizationDecision decision = authorizationSimulator.decide(payment.getAmountMinor());

        UUID eventId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId.toString());
        payload.put("eventType", decision.eventType());
        payload.put("paymentId", payment.getId().toString());
        payload.put("idempotencyKey", payment.getIdempotencyKey());
        payload.put("amountMinor", payment.getAmountMinor());
        payload.put("currency", payment.getCurrency());
        payload.put("status", decision.statusName());
        payload.put("failureReason", decision.reason());

        OutboxEvent event = new OutboxEvent(
                eventId,
                payment.getId(),
                "PAYMENT",
                decision.eventType(),
                writeJson(payload)
        );
        outboxEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPayment(UUID id) {
        return paymentRepository.findById(id);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}

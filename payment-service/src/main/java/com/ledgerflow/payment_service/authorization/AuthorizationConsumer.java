package com.ledgerflow.payment_service.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.payment_service.event.PaymentEventEnvelope;
import com.ledgerflow.payment_service.event.ProcessedEvent;
import com.ledgerflow.payment_service.event.ProcessedEventId;
import com.ledgerflow.payment_service.event.ProcessedEventRepository;
import com.ledgerflow.payment_service.payment.Payment;
import com.ledgerflow.payment_service.payment.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

// Subscribes to the same payments.events topic the outbox relay publishes
// to (including this service's own PaymentCreated events, which it ignores)
// and reacts only to the authorization outcome events produced by
// AuthorizationSimulator, flipping the payment's status once they arrive.
@Component
public class AuthorizationConsumer {

    static final String CONSUMER_GROUP = "payment-service-authorization";

    private static final Logger log = LoggerFactory.getLogger(AuthorizationConsumer.class);

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public AuthorizationConsumer(PaymentRepository paymentRepository,
                                  ProcessedEventRepository processedEventRepository,
                                  ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payments.events", groupId = CONSUMER_GROUP)
    public void onMessage(String message) {
        try {
            PaymentEventEnvelope event = objectMapper.readValue(message, PaymentEventEnvelope.class);
            handle(event);
        } catch (Exception e) {
            log.error("Failed to process payments.events message: {}", message, e);
            throw e instanceof RuntimeException re ? re : new IllegalStateException(e);
        }
    }

    @Transactional
    void handle(PaymentEventEnvelope event) {
        ProcessedEventId processedEventId = new ProcessedEventId(event.eventId(), CONSUMER_GROUP);
        if (processedEventRepository.existsById(processedEventId)) {
            log.info("Event {} already processed by {}, skipping", event.eventId(), CONSUMER_GROUP);
            return;
        }

        switch (event.eventType()) {
            case "PaymentAuthorized" -> applyOutcome(event.paymentId(), Payment::markAuthorized);
            case "PaymentAuthorizationFailed" ->
                    applyOutcome(event.paymentId(), payment -> payment.markDeclined(event.failureReason()));
            default -> log.debug("Ignoring event type {}", event.eventType());
        }

        processedEventRepository.save(new ProcessedEvent(processedEventId));
    }

    private void applyOutcome(UUID paymentId, Consumer<Payment> mutator) {
        paymentRepository.findById(paymentId).ifPresentOrElse(
                payment -> {
                    mutator.accept(payment);
                    paymentRepository.save(payment);
                },
                () -> log.warn("Received authorization outcome for unknown payment {}", paymentId)
        );
    }
}

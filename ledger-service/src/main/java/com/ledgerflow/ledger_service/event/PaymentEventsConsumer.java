package com.ledgerflow.ledger_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    private final PaymentEventProcessor paymentEventProcessor;
    private final ObjectMapper objectMapper;

    public PaymentEventsConsumer(PaymentEventProcessor paymentEventProcessor, ObjectMapper objectMapper) {
        this.paymentEventProcessor = paymentEventProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payments.events", groupId = PaymentEventProcessor.CONSUMER_GROUP)
    public void onMessage(String message) {
        try {
            PaymentEventEnvelope event = objectMapper.readValue(message, PaymentEventEnvelope.class);
            paymentEventProcessor.handle(event);
        } catch (Exception e) {
            log.error("Failed to process payments.events message: {}", message, e);
            throw e instanceof RuntimeException re ? re : new IllegalStateException(e);
        }
    }
}

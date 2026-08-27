package com.ledgerflow.ledger_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Terminal handler for payments.dlt. Never re-attempts processing (that's
// the whole point of a DLQ) — just records the poison message so it's
// queryable instead of silently lost or buried only in logs.
@Component
public class DeadLetterHandler {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterHandler.class);

    private final DeadLetterRepository deadLetterRepository;

    public DeadLetterHandler(DeadLetterRepository deadLetterRepository) {
        this.deadLetterRepository = deadLetterRepository;
    }

    @KafkaListener(topics = "payments.dlt", groupId = "ledger-service-dlt")
    public void onMessage(String message,
                           @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic,
                           @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) String exceptionMessage) {
        log.error("Dead-lettered message from {}: {} (payload: {})", originalTopic, exceptionMessage, message);
        deadLetterRepository.save(new DeadLetter(UUID.randomUUID(), originalTopic, message, exceptionMessage));
    }
}

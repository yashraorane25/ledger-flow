package com.ledgerflow.payment_service.event;

import java.util.UUID;

public record PaymentEventEnvelope(
        UUID eventId,
        String eventType,
        UUID paymentId,
        String idempotencyKey,
        long amountMinor,
        String currency,
        String status,
        String failureReason
) {
}

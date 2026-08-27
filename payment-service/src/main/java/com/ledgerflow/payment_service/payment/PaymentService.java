package com.ledgerflow.payment_service.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.payment_service.outbox.OutboxEvent;
import com.ledgerflow.payment_service.outbox.OutboxEventRepository;
import com.ledgerflow.payment_service.payment.dto.CreatePaymentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                           OutboxEventRepository outboxEventRepository,
                           ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
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

        return payment;
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

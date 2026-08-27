package com.ledgerflow.payment_service.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(
        @NotBlank String idempotencyKey,
        @Positive long amountMinor,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}

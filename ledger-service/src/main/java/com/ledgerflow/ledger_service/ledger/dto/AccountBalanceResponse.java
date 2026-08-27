package com.ledgerflow.ledger_service.ledger.dto;

public record AccountBalanceResponse(
        String accountCode,
        String accountType,
        String currency,
        long balanceMinor,
        boolean cached
) {
}

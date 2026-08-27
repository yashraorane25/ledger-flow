package com.ledgerflow.ledger_service.ledger.dto;

import com.ledgerflow.ledger_service.ledger.LedgerAccount;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountCode,
        String accountType,
        String currency
) {
    public static AccountResponse from(LedgerAccount account) {
        return new AccountResponse(account.getId(), account.getAccountCode(), account.getAccountType(), account.getCurrency());
    }
}

package com.ledgerflow.payment_service.authorization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Stands in for a real PSP/acquirer call, which this demo project doesn't
// have. Decision is deterministic (by amount) rather than random, so both
// the authorized and declined paths can be demoed reliably.
@Component
public class AuthorizationSimulator {

    private final long declineThresholdMinor;

    public AuthorizationSimulator(
            @Value("${ledgerflow.authorization.decline-threshold-minor:1000000}") long declineThresholdMinor) {
        this.declineThresholdMinor = declineThresholdMinor;
    }

    public AuthorizationDecision decide(long amountMinor) {
        if (amountMinor > declineThresholdMinor) {
            return AuthorizationDecision.declined("AMOUNT_EXCEEDS_LIMIT");
        }
        return AuthorizationDecision.success();
    }
}

package com.ledgerflow.payment_service.authorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationSimulatorTest {

    private final AuthorizationSimulator simulator = new AuthorizationSimulator(1_000_000L);

    @Test
    void amountAtOrBelowThresholdIsAuthorized() {
        assertTrue(simulator.decide(1_000_000L).authorized());
        assertTrue(simulator.decide(1L).authorized());
    }

    @Test
    void amountAboveThresholdIsDeclined() {
        AuthorizationDecision decision = simulator.decide(1_000_001L);

        assertFalse(decision.authorized());
        assertEquals("AMOUNT_EXCEEDS_LIMIT", decision.reason());
        assertEquals("PaymentAuthorizationFailed", decision.eventType());
        assertEquals("FAILED", decision.statusName());
    }
}

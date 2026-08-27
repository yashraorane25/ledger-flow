package com.ledgerflow.payment_service.authorization;

public record AuthorizationDecision(boolean authorized, String reason) {

    public static AuthorizationDecision success() {
        return new AuthorizationDecision(true, null);
    }

    public static AuthorizationDecision declined(String reason) {
        return new AuthorizationDecision(false, reason);
    }

    public String eventType() {
        return authorized ? "PaymentAuthorized" : "PaymentAuthorizationFailed";
    }

    public String statusName() {
        return authorized ? "AUTHORIZED" : "FAILED";
    }
}

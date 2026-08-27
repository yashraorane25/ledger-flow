-- Also created by ledger-service's V1__ledger.sql. Both services share one
-- physical Postgres schema in this project, and each service's own consumer
-- (payment-service's AuthorizationConsumer, ledger-service's PaymentEventsConsumer)
-- writes rows here keyed by (event_id, consumer_group), so either service may
-- be the one whose Flyway migration actually creates it first. IF NOT EXISTS
-- keeps that safe regardless of startup order, and lets payment-service run
-- standalone (e.g. in its own isolated Testcontainers database) too.
CREATE TABLE IF NOT EXISTS processed_events (
    event_id       UUID        NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);

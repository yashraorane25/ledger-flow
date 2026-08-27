CREATE TABLE payments (
    id                UUID PRIMARY KEY,
    idempotency_key   VARCHAR(100) NOT NULL UNIQUE,
    amount_minor      BIGINT       NOT NULL CHECK (amount_minor > 0),
    currency          CHAR(3)      NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    failure_reason    VARCHAR(255),
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE outbox (
    id             UUID PRIMARY KEY,
    aggregate_id   UUID         NOT NULL,
    aggregate_type VARCHAR(50)  NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished
    ON outbox (created_at)
    WHERE published_at IS NULL;
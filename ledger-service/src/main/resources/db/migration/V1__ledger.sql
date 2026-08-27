CREATE TABLE ledger_accounts (
    id           UUID PRIMARY KEY,
    account_code VARCHAR(50)  NOT NULL UNIQUE,
    account_type VARCHAR(20)  NOT NULL,
    currency     CHAR(3)      NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE journals (
    id           UUID PRIMARY KEY,
    payment_id   UUID         NOT NULL,
    event_type   VARCHAR(50)  NOT NULL,
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_journals_payment ON journals (payment_id);

CREATE TABLE ledger_entries (
    id           UUID PRIMARY KEY,
    journal_id   UUID        NOT NULL REFERENCES journals (id),
    account_id   UUID        NOT NULL REFERENCES ledger_accounts (id),
    direction    CHAR(2)     NOT NULL CHECK (direction IN ('DR', 'CR')),
    amount_minor BIGINT      NOT NULL CHECK (amount_minor > 0),
    currency     CHAR(3)     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_entries_account ON ledger_entries (account_id);
CREATE INDEX idx_entries_journal ON ledger_entries (journal_id);

CREATE TABLE processed_events (
    event_id       UUID        NOT NULL,
    consumer_group VARCHAR(100) NOT NULL,
    processed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer_group)
);

CREATE TABLE reconciliation_reports (
    id            UUID        PRIMARY KEY,
    run_date      DATE        NOT NULL,
    payment_id    UUID,
    mismatch_type VARCHAR(50) NOT NULL,
    expected_minor BIGINT,
    actual_minor   BIGINT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recon_run_date ON reconciliation_reports (run_date);

INSERT INTO ledger_accounts (id, account_code, account_type, currency) VALUES
    (gen_random_uuid(), 'CASH',              'ASSET',     'USD'),
    (gen_random_uuid(), 'MERCHANT_PAYABLE',  'LIABILITY', 'USD'),
    (gen_random_uuid(), 'PSP_RECEIVABLE',    'ASSET',     'USD'),
    (gen_random_uuid(), 'FEE_REVENUE',       'REVENUE',   'USD');
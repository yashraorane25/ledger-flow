CREATE TABLE dead_letters (
    id              UUID        PRIMARY KEY,
    original_topic  VARCHAR(100) NOT NULL,
    event_payload   TEXT,
    error_message   VARCHAR(1000),
    failed_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_dead_letters_failed_at ON dead_letters (failed_at);

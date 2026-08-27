# ledger-flow

Event-driven payment settlement and ledger system. Portfolio project
demonstrating Kafka, transactional outbox, and double-entry accounting.

## Structure
- `payment-service` (port 8080) — REST API, outbox relay, authorization consumer
- `ledger-service` (port 8081) — ledger consumer, balance API, reconciliation, DLQ handler
- `docker-compose.yml` — Kafka (KRaft), PostgreSQL 16, Redis 8

## Stack
Java 21, Spring Boot 4.1, Maven, Flyway, Testcontainers 2.x

## Conventions
- Money stored as BIGINT minor units (cents). Never DECIMAL or float.
- Testcontainers 2.x: imports are `org.testcontainers.kafka.KafkaContainer`,
  NOT `org.testcontainers.containers.*`. Containers need an explicit image.
- Kafka topics: `payments.events` (3 partitions, keyed by payment ID),
  `payments.dlt`
- Every consumer is idempotent via `processed_events` (event_id, consumer_group)
- Journal debits must equal credits — enforced in service layer, asserted in tests

## Build
`./mvnw clean verify` in each service. Docker must be running for tests.
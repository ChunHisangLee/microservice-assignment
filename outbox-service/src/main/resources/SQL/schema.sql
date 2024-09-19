CREATE DATABASE outboxdb;

\c outboxdb

-- Drop outbox's table if it exists
DROP TABLE IF EXISTS outbox CASCADE;

-- Create users' table
CREATE TABLE outbox
(
    id           SERIAL PRIMARY KEY,
    user_id      BIGINT,
    event_type   VARCHAR(255),
    payload      TEXT,
    routing_key  VARCHAR(255),
    created_at   TIMESTAMP,
    processed_at TIMESTAMP,
    status       VARCHAR(255)
);

-- Correct way to create an index
CREATE INDEX idx_status ON outbox (status)

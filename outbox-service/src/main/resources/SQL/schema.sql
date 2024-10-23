CREATE DATABASE outboxdb;

\c outboxdb

-- Drop outbox table if it exists
DROP TABLE IF EXISTS outbox CASCADE;

-- Create outbox table
CREATE TABLE outbox
(
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, -- Auto-incrementing primary key
    aggregate_type  VARCHAR(100) NOT NULL,                           -- Type of aggregate (e.g., Users, Order)
    aggregate_id    VARCHAR(100) NOT NULL,                           -- Unique identifier of the aggregate instance
    event_type      VARCHAR(100) NOT NULL,                           -- Type of event (e.g., USER_CREATED, USER_UPDATED)
    payload         TEXT         NOT NULL,                           -- Payload data in JSON format
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Creation timestamp
    sequence_number BIGINT       NOT NULL,                           -- Sequence number to maintain event order
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',         -- Status of the event (default to PENDING)
    processed       BOOLEAN      NOT NULL DEFAULT FALSE,             -- Flag indicating whether the event has been processed
    processed_at    TIMESTAMP,                                       -- Timestamp when the event was processed
    event_id        VARCHAR(36)  NOT NULL UNIQUE,                    -- Unique identifier for the event
    routing_key     VARCHAR(255) NOT NULL                            -- Routing key for the outbox event
);

-- Create indexes for the outbox table
CREATE INDEX idx_outbox_created_at ON outbox (created_at);
CREATE INDEX idx_outbox_processed ON outbox (status);
CREATE INDEX idx_outbox_aggregate ON outbox (aggregate_type, aggregate_id, sequence_number);

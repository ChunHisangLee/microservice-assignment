CREATE DATABASE userdb;

\c userdb

-- After connecting to the `userdb` database, run the following script:

-- Drop users' table if it exists
DROP TABLE IF EXISTS users CASCADE;

-- Create users' table
CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,
    name     VARCHAR(100)        NOT NULL,
    email    VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255)        NOT NULL,
    CONSTRAINT email_unique UNIQUE (email)
);

-- Create indexes
CREATE INDEX idx_email ON users (email);


-- Drop outbox's table if it exists
DROP TABLE IF EXISTS outbox CASCADE;
CREATE TABLE outbox
(
    id             BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    aggregate_id   BIGINT       NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed      BOOLEAN               DEFAULT FALSE,
    processed_at   TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_aggregate_type ON outbox (aggregate_type);
CREATE INDEX idx_processed ON outbox (processed);

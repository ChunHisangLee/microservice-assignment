CREATE DATABASE transactiondb;

\c transactiondb

-- Drop transactions' table if it exists
DROP TABLE IF EXISTS transactions CASCADE;

-- Create transactions table
CREATE TABLE transactions
(
    id                   SERIAL PRIMARY KEY,
    user_id              BIGINT                          NOT NULL,
    btc_price_history_id BIGINT                          NOT NULL,
    btc_amount           NUMERIC CHECK (btc_amount >= 0) NOT NULL,  -- BigDecimal type is represented as NUMERIC in SQL
    usd_amount           NUMERIC CHECK (usd_amount >= 0) NOT NULL,  -- New field for USD amount
    transaction_time     TIMESTAMP                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transaction_type     VARCHAR(255)                    NOT NULL,
    CONSTRAINT chk_transaction_time CHECK (transaction_time <= CURRENT_TIMESTAMP)
);

-- Creating indexes based on your entity's index annotations
CREATE INDEX idx_user_id ON transactions (user_id);
CREATE INDEX idx_btc_price_history_id ON transactions (btc_price_history_id);

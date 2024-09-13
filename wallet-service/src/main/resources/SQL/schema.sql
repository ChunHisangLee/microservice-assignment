-- Create the `walletdb` database if it doesn't already exist
CREATE DATABASE walletdb;

-- Connect to `walletdb`
\c walletdb

-- Drop the wallet table if it exists
DROP TABLE IF EXISTS wallet CASCADE;

-- Create the wallet table
CREATE TABLE wallet
(
    id          SERIAL PRIMARY KEY,                                                   -- Auto-incrementing ID for the wallet
    user_id     INTEGER                    NOT NULL,                                  -- User ID associated with the wallet
    usd_balance DECIMAL(18, 2) DEFAULT 0.00 NOT NULL CHECK (usd_balance >= 0),         -- USD balance with 2 decimal precision, non-negative
    btc_balance DECIMAL(18, 8) DEFAULT 0.00000000 NOT NULL CHECK (btc_balance >= 0),   -- BTC balance with 8 decimal precision, non-negative
    CONSTRAINT check_usd_balance CHECK (usd_balance >= 0),                            -- Ensure USD balance is non-negative
    CONSTRAINT check_btc_balance CHECK (btc_balance >= 0)                             -- Ensure BTC balance is non-negative
);

-- Create an index for faster lookup by user_id
CREATE INDEX idx_user_id ON wallet (user_id);

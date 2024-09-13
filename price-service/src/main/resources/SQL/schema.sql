CREATE DATABASE pricedb;

\c pricedb

-- After connecting to the `pricedb` database, run the following script:

-- Drop btc_price_history table if exists
DROP TABLE IF EXISTS btc_price_history CASCADE;

-- Create btc_price_history table
CREATE TABLE btc_price_history
(
    id        SERIAL PRIMARY KEY,
    price     DECIMAL(18, 8) NOT NULL CHECK (price >= 0),
    timestamp TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

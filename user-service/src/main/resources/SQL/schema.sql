-- Create the userdb database
CREATE DATABASE userdb;

-- Connect to the userdb database
\c userdb

-- Drop users table if it exists
DROP TABLE IF EXISTS users CASCADE;

-- Create users table
CREATE TABLE users
(
    id       SERIAL PRIMARY KEY,           -- Auto-incrementing primary key
    name     VARCHAR(100)        NOT NULL, -- User's name with a max length of 100 characters
    email    VARCHAR(255) UNIQUE NOT NULL, -- User's email with uniqueness constraint
    password VARCHAR(255)        NOT NULL  -- User's password
);

-- Create index for email column
CREATE INDEX idx_email ON users (email);

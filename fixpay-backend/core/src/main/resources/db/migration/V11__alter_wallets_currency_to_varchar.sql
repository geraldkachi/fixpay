-- V11: convert wallets.currency from CHAR(3) to VARCHAR(3) to align with JPA mapping
ALTER TABLE wallets ALTER COLUMN currency TYPE VARCHAR(3);

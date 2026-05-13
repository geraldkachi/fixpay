-- V13: fix ledger_entries.currency column type
-- CHAR(3) stored as bpchar in PostgreSQL; Hibernate validates String fields as varchar.
-- Alter to varchar(3) to match the JPA entity mapping.

ALTER TABLE ledger_entries ALTER COLUMN currency TYPE VARCHAR(3);

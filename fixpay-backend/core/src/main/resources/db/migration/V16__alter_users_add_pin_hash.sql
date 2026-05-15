-- V16: store bcrypt-hashed transaction PIN on the user record
ALTER TABLE users ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255);

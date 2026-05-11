-- Keycloak needs its own database in the same PostgreSQL instance.
-- This script runs once on first container initialisation.
CREATE DATABASE keycloak_db;
\c keycloak_db
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO fixpay;

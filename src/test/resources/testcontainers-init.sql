-- Espelha initdb/*.sql da raiz do monorepo (só a parte de workbox_service — este
-- container não roda budget-service). Roda como o superusuário do container
-- (POSTGRES_USER/POSTGRES_PASSWORD do PostgreSQLContainer), igual em produção.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE ROLE workbox_service WITH LOGIN PASSWORD 'workbox_service';
GRANT CONNECT ON DATABASE workbox TO workbox_service;
CREATE SCHEMA IF NOT EXISTS workbox AUTHORIZATION workbox_service;

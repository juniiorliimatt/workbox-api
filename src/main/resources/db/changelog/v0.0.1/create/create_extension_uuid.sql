-- liquibase formatted sql

-- changeset junior.lima:create_extension-00 context:structure
-- comment create extension pgcrypto
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_extension pe where UPPER(pe.extname) = UPPER('pgcrypto');
-- Author: Junior Lima
-- Date: 2024-11-23
-- Description: Cria a extensão 'pgcrypto'
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- changeset junior.lima:create_extension-01 context:structure
-- comment create extension uuid-ossp
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_extension pe where UPPER(pe.extname) = UPPER('uuid-ossp');
-- Author: Junior Lima
-- Date: 2024-11-23
-- Description: Cria a extensão 'uuid-ossp'
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
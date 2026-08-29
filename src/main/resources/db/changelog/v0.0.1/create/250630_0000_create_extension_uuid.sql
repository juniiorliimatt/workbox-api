-- liquibase formatted sql

-- changeset oojuniin:create-extension-pgcrypto context:structure
-- comment: Cria a extensão 'pgcrypto' se ela não existir.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- rollback DROP EXTENSION IF EXISTS "pgcrypto";


-- changeset oojuniin:create-extension-uuid-ossp context:structure
-- comment: Cria a extensão 'uuid-ossp' se ela não existir.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- rollback DROP EXTENSION IF EXISTS "uuid-ossp";



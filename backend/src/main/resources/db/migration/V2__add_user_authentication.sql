ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100) NOT NULL,
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email))),
    ADD CONSTRAINT ck_users_email_not_blank CHECK (btrim(email) <> ''),
    ADD CONSTRAINT ck_users_nickname_not_blank CHECK (btrim(nickname) <> ''),
    ADD CONSTRAINT ck_users_password_hash_not_blank CHECK (btrim(password_hash) <> ''),
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN')),
    ADD CONSTRAINT ck_users_version_non_negative CHECK (version >= 0);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        WHERE btrim(email) = '' OR btrim(nickname) = ''
    ) THEN
        RAISE EXCEPTION 'Cannot add authentication: users contain blank emails or nicknames';
    END IF;

    IF EXISTS (
        SELECT lower(btrim(email))
        FROM users
        GROUP BY lower(btrim(email))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize user emails: normalized values are duplicated';
    END IF;

    IF EXISTS (
        SELECT btrim(nickname)
        FROM users
        GROUP BY btrim(nickname)
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize user nicknames: trimmed values are duplicated';
    END IF;
END $$;

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100),
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE users
SET email = lower(btrim(email)),
    nickname = btrim(nickname),
    password_hash = '!PASSWORD_RESET_REQUIRED!';

ALTER TABLE users
    ALTER COLUMN password_hash SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE users
    ADD CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email))),
    ADD CONSTRAINT ck_users_email_not_blank CHECK (btrim(email) <> ''),
    ADD CONSTRAINT ck_users_nickname_not_blank CHECK (btrim(nickname) <> ''),
    ADD CONSTRAINT ck_users_password_hash_not_blank CHECK (btrim(password_hash) <> ''),
    ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN')),
    ADD CONSTRAINT ck_users_version_non_negative CHECK (version >= 0);

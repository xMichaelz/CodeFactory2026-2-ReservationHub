-- #202 bloqueo por intentos fallidos de login
ALTER TABLE users ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMPTZ;
ALTER TABLE users ADD CONSTRAINT ck_users_failed_attempts CHECK (failed_attempts >= 0);

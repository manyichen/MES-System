-- Authentication and role-management migration for the MES cloud PostgreSQL database.
-- This script is idempotent. It adds login fields without deleting existing data.

ALTER TABLE mes_user
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

ALTER TABLE mes_user
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE mes_user
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

ALTER TABLE mes_user
    ALTER COLUMN role_code SET DEFAULT 'PRODUCTION_OPERATOR';

CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_user_username ON mes_user (username);

CREATE INDEX IF NOT EXISTS idx_mes_user_role_code ON mes_user (role_code);

COMMENT ON COLUMN mes_user.password_hash IS 'Password hash for login authentication';
COMMENT ON COLUMN mes_user.updated_at IS 'Last user update time';
COMMENT ON COLUMN mes_user.last_login_at IS 'Last successful login time';

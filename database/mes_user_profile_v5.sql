-- MES v5: personal profile fields shared by all human user roles.
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS email VARCHAR(150);
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000);
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS profile_bio VARCHAR(500);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_user_email
    ON mes_user (lower(email)) WHERE email IS NOT NULL AND email <> '';

COMMENT ON COLUMN mes_user.email IS '用户联系邮箱，由用户本人维护';
COMMENT ON COLUMN mes_user.avatar_url IS '个人头像地址，可使用 HTTPS URL 或 data image URL';
COMMENT ON COLUMN mes_user.profile_bio IS '个人简介，最多 500 字符';

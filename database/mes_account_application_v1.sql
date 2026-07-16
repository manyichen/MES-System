-- Account application workflow: HR submits, system administrator approves or rejects.
CREATE TABLE IF NOT EXISTS mes_account_apply (
    apply_id BIGSERIAL PRIMARY KEY,
    apply_no VARCHAR(50) NOT NULL,
    applicant_id BIGINT,
    username VARCHAR(50) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    phone VARCHAR(30),
    password_hash VARCHAR(255) NOT NULL,
    apply_reason VARCHAR(500),
    apply_status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reviewer_id BIGINT,
    reviewed_at TIMESTAMP,
    review_comment VARCHAR(500),
    created_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_account_apply_apply_no ON mes_account_apply (apply_no);
CREATE INDEX IF NOT EXISTS idx_mes_account_apply_applicant_id ON mes_account_apply (applicant_id);
CREATE INDEX IF NOT EXISTS idx_mes_account_apply_status ON mes_account_apply (apply_status);
CREATE INDEX IF NOT EXISTS idx_mes_account_apply_username_status ON mes_account_apply (lower(username), apply_status);

COMMENT ON TABLE mes_account_apply IS '账号申请表：人事经理发起，系统管理员审核后创建账号';

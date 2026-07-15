-- Shared-field migration for ABC integration on an existing cloud PostgreSQL database.
-- Run this once if the tables were created before product_id/batch_no/work_report_id were added.

ALTER TABLE mes_work_order
    ADD COLUMN IF NOT EXISTS product_id BIGINT;

ALTER TABLE mes_work_order
    ADD COLUMN IF NOT EXISTS batch_no VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_mes_work_order_batch_no
    ON mes_work_order (batch_no);

ALTER TABLE mes_work_report
    ADD COLUMN IF NOT EXISTS batch_no VARCHAR(50);

ALTER TABLE mes_work_report
    ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

ALTER TABLE mes_work_report
    ADD COLUMN IF NOT EXISTS reject_reason VARCHAR(500);

ALTER TABLE mes_quality_inspection
    ADD COLUMN IF NOT EXISTS work_report_id BIGINT;

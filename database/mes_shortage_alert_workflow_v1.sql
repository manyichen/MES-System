-- 缺料预警闭环升级：在已有数据库执行一次。
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS material_id BIGINT;
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS material_code VARCHAR(60);
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS material_name VARCHAR(100);
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS required_qty NUMERIC(18,4) DEFAULT 0;
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS available_qty NUMERIC(18,4) DEFAULT 0;
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS shortage_qty NUMERIC(18,4) DEFAULT 0;
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS accepted_by BIGINT;
ALTER TABLE mes_shortage_alert ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_mes_shortage_alert_task_material_status
    ON mes_shortage_alert (task_id, material_id, alert_status);

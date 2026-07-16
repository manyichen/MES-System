-- 单条轮胎二维码追溯 v7
-- 每条成品轮胎拥有独立序列号、访问令牌、二维码、追溯文件和打印记录。

CREATE TABLE IF NOT EXISTS mes_tire_instance (
    tire_id BIGSERIAL PRIMARY KEY,
    serial_no VARCHAR(80) NOT NULL UNIQUE,
    trace_code VARCHAR(80) NOT NULL UNIQUE,
    work_order_id BIGINT NOT NULL REFERENCES mes_work_order(work_order_id),
    inspection_id BIGINT NOT NULL REFERENCES mes_quality_inspection(inspection_id),
    work_report_id BIGINT REFERENCES mes_work_report(report_id),
    product_id BIGINT REFERENCES mes_product(product_id),
    warehouse_id BIGINT REFERENCES mes_warehouse(warehouse_id),
    location_id BIGINT REFERENCES mes_warehouse_location(location_id),
    batch_no VARCHAR(80),
    tire_status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK',
    qualified_at TIMESTAMP,
    inbound_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES mes_user(user_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mes_tire_instance_work_order ON mes_tire_instance(work_order_id);
CREATE INDEX IF NOT EXISTS idx_mes_tire_instance_inspection ON mes_tire_instance(inspection_id);
CREATE INDEX IF NOT EXISTS idx_mes_tire_instance_batch ON mes_tire_instance(batch_no);
CREATE INDEX IF NOT EXISTS idx_mes_tire_instance_warehouse ON mes_tire_instance(warehouse_id, location_id);

CREATE TABLE IF NOT EXISTS mes_tire_qrcode (
    qrcode_id BIGSERIAL PRIMARY KEY,
    tire_id BIGINT NOT NULL UNIQUE REFERENCES mes_tire_instance(tire_id) ON DELETE CASCADE,
    access_token VARCHAR(64) NOT NULL UNIQUE,
    target_url VARCHAR(1000) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    qrcode_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    print_count INTEGER NOT NULL DEFAULT 0,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_printed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mes_trace_document (
    document_id BIGSERIAL PRIMARY KEY,
    tire_id BIGINT NOT NULL REFERENCES mes_tire_instance(tire_id) ON DELETE CASCADE,
    document_type VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    version_no INTEGER NOT NULL DEFAULT 1,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tire_id, document_type, version_no)
);

CREATE INDEX IF NOT EXISTS idx_mes_trace_document_tire ON mes_trace_document(tire_id);

CREATE TABLE IF NOT EXISTS mes_label_print_task (
    print_task_id BIGSERIAL PRIMARY KEY,
    tire_id BIGINT NOT NULL REFERENCES mes_tire_instance(tire_id),
    printed_by BIGINT REFERENCES mes_user(user_id),
    print_status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    print_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_mes_label_print_tire ON mes_label_print_task(tire_id, print_time);

INSERT INTO mes_permission
    (permission_code, permission_name, module_code, resource_type, action_code, risk_level)
VALUES
    ('trace.tire.generate', '生成单条轮胎二维码标签', 'trace', 'tire_instance', 'generate', 'HIGH'),
    ('trace.tire.print', '打印或补打轮胎二维码标签', 'trace', 'tire_label', 'print', 'HIGH')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    module_code = EXCLUDED.module_code,
    resource_type = EXCLUDED.resource_type,
    action_code = EXCLUDED.action_code,
    risk_level = EXCLUDED.risk_level,
    enabled = 1;

WITH grants(role_code, permission_code) AS (
    VALUES
        ('WAREHOUSE_ADMIN', 'trace.tire.generate'),
        ('WAREHOUSE_ADMIN', 'trace.tire.print')
)
INSERT INTO mes_role_permission(role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM grants g
JOIN mes_role r ON r.role_code = g.role_code
JOIN mes_permission p ON p.permission_code = g.permission_code
ON CONFLICT(role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission(role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r CROSS JOIN mes_permission p
WHERE r.role_code = 'SYSTEM_ADMIN'
  AND p.permission_code IN ('trace.tire.generate', 'trace.tire.print')
ON CONFLICT(role_id, permission_id) DO NOTHING;

-- 3万条轮胎二维码/追溯演示数据导入表
-- 先在 PostgreSQL 执行本文件建表，然后使用 psql 的 \copy 导入 CSV。

CREATE TABLE IF NOT EXISTS mes_tire_trace_demo_30000 (
    trace_code varchar(64) PRIMARY KEY,
    qr_code_value varchar(128) NOT NULL,
    batch_no varchar(64) NOT NULL,
    serial_no varchar(64) NOT NULL,
    order_no varchar(64) NOT NULL,
    customer_name varchar(128) NOT NULL,
    product_code varchar(64) NOT NULL,
    product_name varchar(128) NOT NULL,
    tire_spec varchar(64) NOT NULL,
    product_category varchar(32) NOT NULL,
    work_order_no varchar(64) NOT NULL,
    process_stage varchar(64) NOT NULL,
    line_code varchar(64) NOT NULL,
    equipment_code varchar(64) NOT NULL,
    operator_no varchar(64) NOT NULL,
    material_batches text NOT NULL,
    inspection_result varchar(16) NOT NULL,
    defect_desc varchar(128) NOT NULL,
    rfv_n numeric(10,2) NOT NULL,
    lfv_n numeric(10,2) NOT NULL,
    dynamic_balance_g numeric(10,2) NOT NULL,
    appearance_grade varchar(8) NOT NULL,
    current_status varchar(32) NOT NULL,
    produced_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tire_trace_demo_batch ON mes_tire_trace_demo_30000(batch_no);
CREATE INDEX IF NOT EXISTS idx_tire_trace_demo_order ON mes_tire_trace_demo_30000(order_no);
CREATE INDEX IF NOT EXISTS idx_tire_trace_demo_product ON mes_tire_trace_demo_30000(product_code);
CREATE INDEX IF NOT EXISTS idx_tire_trace_demo_quality ON mes_tire_trace_demo_30000(inspection_result, current_status);

-- psql 示例：
-- \copy mes_tire_trace_demo_30000 FROM 'database/demo_dataset/mes_tire_trace_30000.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
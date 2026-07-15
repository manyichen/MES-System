-- MES warehouse inventory demo seed.
-- Safe to run repeatedly. It creates visible warehouse stock for material requisition tests.

BEGIN;

CREATE TABLE IF NOT EXISTS mes_user_warehouse_scope (
    user_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, warehouse_id)
);

CREATE INDEX IF NOT EXISTS idx_mes_user_warehouse_scope_warehouse_id
    ON mes_user_warehouse_scope (warehouse_id);

ALTER TABLE mes_robot ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_mes_robot_warehouse_id
    ON mes_robot (warehouse_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_material_material_code
    ON mes_material (material_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_warehouse_warehouse_code
    ON mes_warehouse (warehouse_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_warehouse_location_location_code
    ON mes_warehouse_location (location_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_inventory_transaction_transaction_no
    ON mes_inventory_transaction (transaction_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_robot_robot_code
    ON mes_robot (robot_code);

INSERT INTO mes_warehouse (warehouse_code, warehouse_name, warehouse_type, enabled)
VALUES
    ('WH-RAW-01', '原材料仓', 'RAW', 1),
    ('WH-WIP-01', '半成品暂存仓', 'WIP', 1),
    ('WH-FG-01', '成品仓', 'FINISHED', 1)
ON CONFLICT (warehouse_code) DO UPDATE SET
    warehouse_name = EXCLUDED.warehouse_name,
    warehouse_type = EXCLUDED.warehouse_type,
    enabled = EXCLUDED.enabled;

INSERT INTO mes_warehouse_location (warehouse_id, location_code, location_name, enabled)
SELECT w.warehouse_id, v.location_code, v.location_name, 1
FROM (VALUES
    ('WH-RAW-01', 'RAW-A01', '原材料 A01'),
    ('WH-RAW-01', 'RAW-A02', '原材料 A02'),
    ('WH-RAW-01', 'RAW-B01', '原材料 B01'),
    ('WH-WIP-01', 'WIP-B01', '半成品 B01'),
    ('WH-FG-01', 'FG-C01', '成品 C01')
) AS v(warehouse_code, location_code, location_name)
JOIN mes_warehouse w ON w.warehouse_code = v.warehouse_code
ON CONFLICT (location_code) DO UPDATE SET
    warehouse_id = EXCLUDED.warehouse_id,
    location_name = EXCLUDED.location_name,
    enabled = EXCLUDED.enabled;

INSERT INTO mes_material (
    material_code,
    material_name,
    material_type,
    specification,
    unit,
    shelf_life_days,
    enabled
)
VALUES
    ('MAT-NR-RSS3', '天然橡胶 RSS3', 'RAW', 'RSS3', 'kg', 365, 1),
    ('MAT-SBR-1502', '丁苯橡胶 SBR1502', 'RAW', 'SBR1502', 'kg', 365, 1),
    ('MAT-CB-N330', '炭黑 N330', 'RAW', 'N330', 'kg', 540, 1),
    ('MAT-STEEL-CORD', '钢丝帘线', 'RAW', '0.30mm', 'kg', 720, 1),
    ('MAT-SULFUR', '硫磺', 'AUX', '200目', 'kg', 365, 1),
    ('MAT-ANTI-AGE', '防老剂 4020', 'AUX', '4020', 'kg', 365, 1),
    ('MAT-CURING-BAG', '硫化胶囊', 'AUX', 'R16/R17通用', 'pcs', 180, 1)
ON CONFLICT (material_code) DO UPDATE SET
    material_name = EXCLUDED.material_name,
    material_type = EXCLUDED.material_type,
    specification = EXCLUDED.specification,
    unit = EXCLUDED.unit,
    shelf_life_days = EXCLUDED.shelf_life_days,
    enabled = EXCLUDED.enabled;

WITH inventory_seed(material_code, warehouse_code, location_code, batch_no, available_qty) AS (
    VALUES
        ('MAT-NR-RSS3', 'WH-RAW-01', 'RAW-A01', 'BATCH-DEMO-001', 5000.0000),
        ('MAT-NR-RSS3', 'WH-RAW-01', 'RAW-A01', 'BATCH-NR-202607', 6000.0000),
        ('MAT-SBR-1502', 'WH-RAW-01', 'RAW-A01', 'BATCH-SBR-202607', 3800.0000),
        ('MAT-CB-N330', 'WH-RAW-01', 'RAW-A02', 'BATCH-20260709-001', 3200.0000),
        ('MAT-CB-N330', 'WH-RAW-01', 'RAW-A02', 'BATCH-CB-202607', 3000.0000),
        ('MAT-STEEL-CORD', 'WH-RAW-01', 'RAW-A02', 'BATCH-SC-202607', 2600.0000),
        ('MAT-SULFUR', 'WH-RAW-01', 'RAW-B01', 'BATCH-SUL-202607', 900.0000),
        ('MAT-ANTI-AGE', 'WH-RAW-01', 'RAW-B01', 'BATCH-AA-202607', 750.0000),
        ('MAT-CURING-BAG', 'WH-WIP-01', 'WIP-B01', 'BATCH-BAG-202607', 120.0000)
),
resolved_seed AS (
    SELECT
        m.material_id,
        w.warehouse_id,
        l.location_id,
        s.batch_no,
        s.available_qty
    FROM inventory_seed s
    JOIN mes_material m ON m.material_code = s.material_code
    JOIN mes_warehouse w ON w.warehouse_code = s.warehouse_code
    JOIN mes_warehouse_location l
        ON l.location_code = s.location_code
       AND l.warehouse_id = w.warehouse_id
),
updated_inventory AS (
    UPDATE mes_inventory i
    SET available_qty = GREATEST(i.available_qty, s.available_qty),
        quality_status = 'QUALIFIED',
        last_check_time = CURRENT_TIMESTAMP
    FROM resolved_seed s
    WHERE i.material_id = s.material_id
      AND i.warehouse_id = s.warehouse_id
      AND i.location_id = s.location_id
      AND i.batch_no = s.batch_no
    RETURNING i.inventory_id
)
INSERT INTO mes_inventory (
    material_id,
    warehouse_id,
    location_id,
    batch_no,
    available_qty,
    reserved_qty,
    frozen_qty,
    quality_status,
    last_check_time
)
SELECT
    s.material_id,
    s.warehouse_id,
    s.location_id,
    s.batch_no,
    s.available_qty,
    0,
    0,
    'QUALIFIED',
    CURRENT_TIMESTAMP
FROM resolved_seed s
WHERE NOT EXISTS (
    SELECT 1
    FROM mes_inventory i
    WHERE i.material_id = s.material_id
      AND i.warehouse_id = s.warehouse_id
      AND i.location_id = s.location_id
      AND i.batch_no = s.batch_no
);

WITH transaction_seed(transaction_no, material_code, warehouse_code, location_code, batch_no, qty) AS (
    VALUES
        ('TX-SEED-NR-DEMO-001', 'MAT-NR-RSS3', 'WH-RAW-01', 'RAW-A01', 'BATCH-DEMO-001', 5000.0000),
        ('TX-SEED-NR-202607', 'MAT-NR-RSS3', 'WH-RAW-01', 'RAW-A01', 'BATCH-NR-202607', 6000.0000),
        ('TX-SEED-SBR-202607', 'MAT-SBR-1502', 'WH-RAW-01', 'RAW-A01', 'BATCH-SBR-202607', 3800.0000),
        ('TX-SEED-CB-DEMO-001', 'MAT-CB-N330', 'WH-RAW-01', 'RAW-A02', 'BATCH-20260709-001', 3200.0000),
        ('TX-SEED-CB-202607', 'MAT-CB-N330', 'WH-RAW-01', 'RAW-A02', 'BATCH-CB-202607', 3000.0000),
        ('TX-SEED-SC-202607', 'MAT-STEEL-CORD', 'WH-RAW-01', 'RAW-A02', 'BATCH-SC-202607', 2600.0000),
        ('TX-SEED-SUL-202607', 'MAT-SULFUR', 'WH-RAW-01', 'RAW-B01', 'BATCH-SUL-202607', 900.0000),
        ('TX-SEED-AA-202607', 'MAT-ANTI-AGE', 'WH-RAW-01', 'RAW-B01', 'BATCH-AA-202607', 750.0000),
        ('TX-SEED-BAG-202607', 'MAT-CURING-BAG', 'WH-WIP-01', 'WIP-B01', 'BATCH-BAG-202607', 120.0000)
),
resolved_transactions AS (
    SELECT
        s.transaction_no,
        m.material_id,
        i.inventory_id,
        s.qty,
        COALESCE(
            (SELECT u.user_id FROM mes_user u WHERE u.role_code = 'WAREHOUSE_ADMIN' ORDER BY u.user_id LIMIT 1),
            (SELECT u.user_id FROM mes_user u ORDER BY u.user_id LIMIT 1),
            1
        ) AS operator_id
    FROM transaction_seed s
    JOIN mes_material m ON m.material_code = s.material_code
    JOIN mes_warehouse w ON w.warehouse_code = s.warehouse_code
    JOIN mes_warehouse_location l
        ON l.location_code = s.location_code
       AND l.warehouse_id = w.warehouse_id
    JOIN mes_inventory i
        ON i.material_id = m.material_id
       AND i.warehouse_id = w.warehouse_id
       AND i.location_id = l.location_id
       AND i.batch_no = s.batch_no
)
INSERT INTO mes_inventory_transaction (
    transaction_no,
    material_id,
    inventory_id,
    transaction_type,
    qty,
    source_doc_type,
    source_doc_id,
    operator_id
)
SELECT
    transaction_no,
    material_id,
    inventory_id,
    'IN',
    qty,
    'DEMO_SEED',
    inventory_id,
    operator_id
FROM resolved_transactions
ON CONFLICT (transaction_no) DO NOTHING;

INSERT INTO mes_robot (
    robot_code,
    robot_name,
    warehouse_id,
    robot_status,
    battery_level,
    current_location,
    enabled
)
SELECT
    v.robot_code,
    v.robot_name,
    w.warehouse_id,
    v.robot_status,
    v.battery_level,
    v.current_location,
    1
FROM (VALUES
    ('ROB-RAW-01', '原材料仓配送机器人一号', 'WH-RAW-01', 'IDLE', 88.00, '原材料仓待命区'),
    ('ROB-RAW-02', '原材料仓配送机器人二号', 'WH-RAW-01', 'IDLE', 76.00, '原材料仓充电区'),
    ('ROB-WIP-01', '半成品转运机器人', 'WH-WIP-01', 'IDLE', 91.00, '半成品暂存仓')
) AS v(robot_code, robot_name, warehouse_code, robot_status, battery_level, current_location)
JOIN mes_warehouse w ON w.warehouse_code = v.warehouse_code
ON CONFLICT (robot_code) DO UPDATE SET
    robot_name = EXCLUDED.robot_name,
    warehouse_id = EXCLUDED.warehouse_id,
    robot_status = EXCLUDED.robot_status,
    battery_level = EXCLUDED.battery_level,
    current_location = EXCLUDED.current_location,
    enabled = EXCLUDED.enabled;

INSERT INTO mes_user_warehouse_scope (user_id, warehouse_id)
SELECT DISTINCT u.user_id, w.warehouse_id
FROM mes_user u
CROSS JOIN mes_warehouse w
WHERE u.role_code = 'WAREHOUSE_ADMIN'
  AND w.warehouse_code IN ('WH-RAW-01', 'WH-WIP-01', 'WH-FG-01')
ON CONFLICT (user_id, warehouse_id) DO NOTHING;

DO $$
BEGIN
    IF to_regclass('public.mes_user_role') IS NOT NULL
       AND to_regclass('public.mes_role') IS NOT NULL THEN
        EXECUTE $sql$
            INSERT INTO mes_user_warehouse_scope (user_id, warehouse_id)
            SELECT DISTINCT u.user_id, w.warehouse_id
            FROM mes_user u
            JOIN mes_user_role ur ON ur.user_id = u.user_id
            JOIN mes_role r ON r.role_id = ur.role_id
            CROSS JOIN mes_warehouse w
            WHERE r.role_code = 'WAREHOUSE_ADMIN'
              AND w.warehouse_code IN ('WH-RAW-01', 'WH-WIP-01', 'WH-FG-01')
            ON CONFLICT (user_id, warehouse_id) DO NOTHING
        $sql$;
    END IF;
END $$;

COMMIT;

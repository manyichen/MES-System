-- 答辩环境就绪修复：返工重排表、已确认的重复/孤儿数据以及关键唯一性约束。
-- 执行前必须先完成 pg_dump 备份。本脚本可重复执行。

BEGIN;

CREATE TABLE IF NOT EXISTS mes_schema_migration (
    version_code VARCHAR(80) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mes_rework_plan_link (
    rework_order_id BIGINT PRIMARY KEY,
    production_task_id BIGINT NOT NULL UNIQUE,
    planned_by BIGINT NOT NULL,
    planned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mes_rework_plan_link_task
    ON mes_rework_plan_link (production_task_id);

INSERT INTO mes_permission
    (permission_code, permission_name, module_code, resource_type, action_code, risk_level)
VALUES
    ('planning.rework.read', '查看待排返工需求', 'planning', 'rework_plan', 'read', 'LOW'),
    ('planning.rework.plan', '将返工需求纳入生产计划', 'planning', 'rework_plan', 'create', 'HIGH')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    module_code = EXCLUDED.module_code,
    resource_type = EXCLUDED.resource_type,
    action_code = EXCLUDED.action_code,
    risk_level = EXCLUDED.risk_level,
    enabled = 1;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('planning.rework.read', 'planning.rework.plan')
WHERE r.role_code IN ('PMC_PLANNER', 'SUPER_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- process_id=21 是未被工单引用的测试工序，与答辩主产品的正式首工序 process_id=1 冲突。
DELETE FROM mes_process_route duplicate_route
WHERE duplicate_route.process_id = 21
  AND duplicate_route.process_seq = 1
  AND NOT EXISTS (
      SELECT 1 FROM mes_work_order work_order
      WHERE work_order.process_id = duplicate_route.process_id
  )
  AND EXISTS (
      SELECT 1
      FROM mes_process_route retained_route
      WHERE retained_route.process_id = 1
        AND retained_route.product_id = duplicate_route.product_id
        AND retained_route.process_seq = duplicate_route.process_seq
        AND retained_route.enabled = 1
  );

-- inventory_id=189 是无流水的重复初始库存；保留已有出库流水且余额为 280 的 inventory_id=183。
DELETE FROM mes_inventory duplicate_inventory
WHERE duplicate_inventory.inventory_id = 189
  AND NOT EXISTS (
      SELECT 1 FROM mes_inventory_transaction transaction_row
      WHERE transaction_row.inventory_id = duplicate_inventory.inventory_id
  )
  AND EXISTS (
      SELECT 1
      FROM mes_inventory retained_inventory
      WHERE retained_inventory.inventory_id = 183
        AND retained_inventory.material_id = duplicate_inventory.material_id
        AND retained_inventory.warehouse_id = duplicate_inventory.warehouse_id
        AND retained_inventory.location_id = duplicate_inventory.location_id
        AND retained_inventory.batch_no = duplicate_inventory.batch_no
        AND retained_inventory.quality_status = duplicate_inventory.quality_status
  );

-- 清理 PlanningServiceTest 历史运行留下的、已无物料主数据的库存及其流水。
DELETE FROM mes_inventory_transaction transaction_row
WHERE transaction_row.inventory_id IN (
    SELECT inventory.inventory_id
    FROM mes_inventory inventory
    LEFT JOIN mes_material material ON material.material_id = inventory.material_id
    WHERE material.material_id IS NULL
      AND inventory.material_id >= 8000000000
);

DELETE FROM mes_inventory inventory
WHERE inventory.material_id >= 8000000000
  AND NOT EXISTS (
      SELECT 1 FROM mes_material material
      WHERE material.material_id = inventory.material_id
  );

-- 一个报工单只应产生一条计件工资，重复执行审核时保留最早记录。
DELETE FROM mes_piecework_wage duplicate_wage
USING mes_piecework_wage retained_wage
WHERE duplicate_wage.report_id = retained_wage.report_id
  AND duplicate_wage.wage_id > retained_wage.wage_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM mes_inventory
        GROUP BY material_id, warehouse_id, location_id, batch_no, quality_status
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION '库存业务键仍有重复记录，拒绝创建唯一索引';
    END IF;
    IF EXISTS (
        SELECT 1 FROM mes_process_route
        WHERE enabled = 1 AND product_id IS NOT NULL
        GROUP BY product_id, process_seq
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION '启用工艺路线仍有顺序冲突，拒绝创建唯一索引';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_inventory_business_key
    ON mes_inventory (material_id, warehouse_id, location_id, batch_no, quality_status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_process_route_product_seq_active
    ON mes_process_route (product_id, process_seq)
    WHERE enabled = 1 AND product_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_piecework_wage_report
    ON mes_piecework_wage (report_id);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mes_inventory_material') THEN
        ALTER TABLE mes_inventory ADD CONSTRAINT fk_mes_inventory_material
            FOREIGN KEY (material_id) REFERENCES mes_material(material_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mes_inventory_warehouse') THEN
        ALTER TABLE mes_inventory ADD CONSTRAINT fk_mes_inventory_warehouse
            FOREIGN KEY (warehouse_id) REFERENCES mes_warehouse(warehouse_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mes_inventory_location') THEN
        ALTER TABLE mes_inventory ADD CONSTRAINT fk_mes_inventory_location
            FOREIGN KEY (location_id) REFERENCES mes_warehouse_location(location_id) ON DELETE RESTRICT;
    END IF;
END $$;

INSERT INTO mes_schema_migration (version_code, description)
VALUES ('v11-super-admin-demo-readiness', '答辩超级管理员全功能展示与数据一致性修复')
ON CONFLICT (version_code) DO UPDATE SET
    description = EXCLUDED.description,
    applied_at = CURRENT_TIMESTAMP;

COMMIT;

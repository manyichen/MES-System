-- MES data scope implementation v4
-- Explicit user assignments replace module-wide fallback access.

CREATE TABLE IF NOT EXISTS mes_user_line_scope (
    user_id BIGINT NOT NULL,
    line_id BIGINT NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, line_id)
);
CREATE INDEX IF NOT EXISTS idx_mes_user_line_scope_line_id
    ON mes_user_line_scope (line_id);

CREATE TABLE IF NOT EXISTS mes_user_warehouse_scope (
    user_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, warehouse_id)
);
CREATE INDEX IF NOT EXISTS idx_mes_user_warehouse_scope_warehouse_id
    ON mes_user_warehouse_scope (warehouse_id);

ALTER TABLE mes_material_requisition ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_mes_material_requisition_warehouse_id
    ON mes_material_requisition (warehouse_id);

ALTER TABLE mes_robot ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_mes_robot_warehouse_id
    ON mes_robot (warehouse_id);

UPDATE mes_material_requisition r
SET warehouse_id = COALESCE(
    (SELECT p.warehouse_id FROM mes_picking_task p
     WHERE p.requisition_id = r.requisition_id ORDER BY p.picking_task_id LIMIT 1),
    (SELECT MIN(w.warehouse_id) FROM mes_warehouse w)
)
WHERE r.warehouse_id IS NULL;

UPDATE mes_robot r
SET warehouse_id = COALESCE(
    (SELECT l.warehouse_id FROM mes_warehouse_location l
     WHERE l.location_code = r.current_location ORDER BY l.location_id LIMIT 1),
    (SELECT MIN(w.warehouse_id) FROM mes_warehouse w)
)
WHERE r.warehouse_id IS NULL;

-- Existing managers receive explicit assignments for current resources only.
INSERT INTO mes_user_line_scope (user_id, line_id)
SELECT DISTINCT u.user_id, l.line_id
FROM mes_user u
JOIN mes_user_role ur ON ur.user_id = u.user_id
JOIN mes_role r ON r.role_id = ur.role_id AND r.role_code = 'WORKSHOP_MANAGER'
CROSS JOIN mes_production_line l
ON CONFLICT (user_id, line_id) DO NOTHING;

INSERT INTO mes_user_warehouse_scope (user_id, warehouse_id)
SELECT DISTINCT u.user_id, w.warehouse_id
FROM mes_user u
JOIN mes_user_role ur ON ur.user_id = u.user_id
JOIN mes_role r ON r.role_id = ur.role_id AND r.role_code = 'WAREHOUSE_ADMIN'
CROSS JOIN mes_warehouse w
ON CONFLICT (user_id, warehouse_id) DO NOTHING;

INSERT INTO mes_permission
    (permission_code, permission_name, module_code, resource_type, action_code, risk_level)
VALUES
    ('data_scope.manage', '分配用户产线和仓库数据范围', 'system', 'data_scope', 'manage', 'CRITICAL')
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
CROSS JOIN mes_permission p
WHERE r.role_code IN ('SYSTEM_ADMIN', 'HR_MANAGER')
  AND p.permission_code = 'data_scope.manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMENT ON TABLE mes_user_line_scope IS '用户产线数据范围：车间管理员只能访问明确分配的产线。';
COMMENT ON TABLE mes_user_warehouse_scope IS '用户仓库数据范围：仓库管理员只能访问明确分配的仓库。';
COMMENT ON COLUMN mes_material_requisition.warehouse_id IS '领料申请目标仓库，用于仓库数据范围过滤。';
COMMENT ON COLUMN mes_robot.warehouse_id IS '机器人所属仓库，用于仓库数据范围过滤。';

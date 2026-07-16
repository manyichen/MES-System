-- PMC 返工重排最小闭环：质量模块确认返工需求，PMC 在计划模块生成新的生产任务。
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
WHERE r.role_code = 'PMC_PLANNER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

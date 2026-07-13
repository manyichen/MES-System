-- MES 权限实施迁移 v3
-- 目标：补齐细粒度权限、角色授权和质检提交审核流程。

ALTER TABLE mes_quality_inspection ADD COLUMN IF NOT EXISTS submitted_by BIGINT;
ALTER TABLE mes_quality_inspection ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE mes_quality_inspection ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE mes_quality_inspection ALTER COLUMN inspector_id DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_mes_quality_inspection_status_assigned
    ON mes_quality_inspection (inspection_status, assigned_to);

INSERT INTO mes_permission
    (permission_code, permission_name, module_code, resource_type, action_code, risk_level)
VALUES
    ('dashboard.system.read', '查看系统运行看板', 'dashboard', 'system_dashboard', 'read', 'HIGH'),
    ('system.health.read', '查看系统健康状态', 'system', 'health', 'read', 'MEDIUM'),
    ('role.read', '查看角色和权限清单', 'system', 'role', 'read', 'MEDIUM'),
    ('role.manage', '维护角色权限', 'system', 'role', 'manage', 'HIGH'),
    ('business.delete', '删除业务数据', 'system', 'business_data', 'delete', 'CRITICAL'),
    ('demo.seed', '生成演示数据', 'system', 'demo_data', 'create', 'HIGH'),

    ('master.read', '查看产品与基础主数据', 'master', 'master_data', 'read', 'LOW'),
    ('master.manage', '维护产品与基础主数据', 'master', 'master_data', 'manage', 'HIGH'),
    ('planning.read', '查看订单计划与全部工单', 'planning', 'plan', 'read', 'LOW'),
    ('planning.work_order.read', '查看本人相关制造工单', 'planning', 'work_order', 'read_own', 'LOW'),
    ('planning.order.create', '创建客户订单', 'planning', 'customer_order', 'create', 'HIGH'),
    ('planning.task.create', '创建生产任务', 'planning', 'production_task', 'create', 'HIGH'),
    ('planning.task.release', '齐套分析并发布生产任务', 'planning', 'production_task', 'release', 'HIGH'),
    ('planning.work_order.create', '创建生产工单', 'planning', 'work_order', 'create', 'HIGH'),
    ('planning.work_order.dispatch', '派发制造工单', 'planning', 'work_order', 'dispatch', 'HIGH'),
    ('planning.work_order.receive', '接收本人制造工单', 'planning', 'work_order', 'receive', 'MEDIUM'),

    ('warehouse.read', '查看仓储物流数据', 'warehouse', 'warehouse_business', 'read', 'LOW'),
    ('warehouse.master.manage', '维护仓库物料库位和机器人', 'warehouse', 'warehouse_master', 'manage', 'HIGH'),
    ('warehouse.requisition.create', '创建领料申请', 'warehouse', 'requisition', 'create', 'MEDIUM'),
    ('warehouse.requisition.approve', '审核领料申请', 'warehouse', 'requisition', 'approve', 'HIGH'),
    ('warehouse.picking.execute', '执行拣货任务', 'warehouse', 'picking_task', 'execute', 'MEDIUM'),
    ('warehouse.delivery.execute', '执行配送和收料交接', 'warehouse', 'delivery_task', 'execute', 'MEDIUM'),
    ('warehouse.inventory.adjust', '调整库存并写入流水', 'warehouse', 'inventory', 'adjust', 'CRITICAL'),

    ('production.read', '查看生产报工', 'production', 'work_report', 'read', 'LOW'),
    ('production.report.create', '提交本人报工', 'production', 'work_report', 'create', 'MEDIUM'),
    ('production.report.update_own', '修改本人待审核报工', 'production', 'work_report', 'update_own', 'MEDIUM'),
    ('production.report.review', '审核报工', 'production', 'work_report', 'review', 'HIGH'),
    ('production.wage.read_self', '查看本人计件工资', 'production', 'piecework_wage', 'read_self', 'MEDIUM'),
    ('production.wage.read_summary', '查看计件工资汇总', 'production', 'piecework_wage', 'read_summary', 'HIGH'),
    ('production.wage.read_all', '查看全部计件工资明细', 'production', 'piecework_wage', 'read_all', 'CRITICAL'),

    ('quality.read', '查看质量数据', 'quality', 'quality_business', 'read', 'LOW'),
    ('quality.inspection.create', '创建质检单', 'quality', 'quality_inspection', 'create', 'HIGH'),
    ('quality.inspection.assign', '分配质检任务', 'quality', 'quality_inspection', 'assign', 'HIGH'),
    ('quality.rework.manage', '创建派发并关闭返工单', 'quality', 'rework_order', 'manage', 'HIGH'),

    ('process.read', '查看工艺标准和参数', 'process', 'process_standard', 'read', 'LOW'),

    ('equipment.read', '查看设备和维修数据', 'equipment', 'equipment_business', 'read', 'LOW'),
    ('equipment.fault.report', '上报设备故障', 'equipment', 'repair_report', 'create', 'MEDIUM'),
    ('equipment.repair.review', '审核设备故障上报', 'equipment', 'repair_report', 'review', 'HIGH'),
    ('equipment.maintenance.assign', '创建并派发维修工单', 'equipment', 'maintenance_order', 'assign', 'HIGH'),
    ('equipment.maintenance.execute', '执行本人维修工单', 'equipment', 'maintenance_order', 'execute', 'MEDIUM'),
    ('equipment.maintenance.accept', '验收维修结果并恢复设备', 'equipment', 'maintenance_order', 'accept', 'HIGH'),

    ('trace.create', '创建产品追溯记录', 'trace', 'product_trace', 'create', 'HIGH'),
    ('feedback.read', '查看管理反馈', 'feedback', 'management_feedback', 'read', 'LOW'),
    ('feedback.create', '提交管理反馈', 'feedback', 'management_feedback', 'create', 'LOW'),
    ('feedback.close', '关闭管理反馈', 'feedback', 'management_feedback', 'close', 'HIGH')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    module_code = EXCLUDED.module_code,
    resource_type = EXCLUDED.resource_type,
    action_code = EXCLUDED.action_code,
    risk_level = EXCLUDED.risk_level,
    enabled = 1;

-- Remove grants left by the earlier coarse-grained matrix that conflict with v3 boundaries.
DELETE FROM mes_role_permission rp
USING mes_role r, mes_permission p
WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id
  AND (
      (r.role_code = 'QUALITY_MANAGER' AND p.permission_code = 'quality.inspect')
      OR (r.role_code = 'SYSTEM_MAINTAINER' AND p.permission_code = 'user.update_role')
      OR (r.role_code = 'VIEWER' AND p.permission_code = 'trace.read')
  );

WITH grants(role_code, permission_code) AS (
    VALUES
    ('SYSTEM_ADMIN','dashboard.read'), ('SYSTEM_ADMIN','dashboard.system.read'),
    ('SYSTEM_ADMIN','system.health.read'), ('SYSTEM_ADMIN','user.read'),
    ('SYSTEM_ADMIN','user.create'), ('SYSTEM_ADMIN','user.update_role'),
    ('SYSTEM_ADMIN','role.read'), ('SYSTEM_ADMIN','role.manage'),
    ('SYSTEM_ADMIN','permission.review'), ('SYSTEM_ADMIN','audit.read'),

    ('HR_MANAGER','dashboard.read'), ('HR_MANAGER','user.read'), ('HR_MANAGER','role.read'),
    ('HR_MANAGER','permission.apply'), ('HR_MANAGER','production.wage.read_all'),

    ('GENERAL_MANAGER','dashboard.read'), ('GENERAL_MANAGER','planning.read'),
    ('GENERAL_MANAGER','warehouse.read'), ('GENERAL_MANAGER','production.read'),
    ('GENERAL_MANAGER','production.wage.read_summary'), ('GENERAL_MANAGER','quality.read'),
    ('GENERAL_MANAGER','process.read'), ('GENERAL_MANAGER','equipment.read'),
    ('GENERAL_MANAGER','trace.read'), ('GENERAL_MANAGER','feedback.read'),
    ('GENERAL_MANAGER','feedback.close'), ('GENERAL_MANAGER','master.read'),

    ('PMC_PLANNER','dashboard.read'), ('PMC_PLANNER','planning.read'),
    ('PMC_PLANNER','planning.order.create'), ('PMC_PLANNER','planning.task.create'),
    ('PMC_PLANNER','planning.task.release'), ('PMC_PLANNER','planning.work_order.create'),
    ('PMC_PLANNER','warehouse.read'), ('PMC_PLANNER','quality.read'),
    ('PMC_PLANNER','equipment.read'), ('PMC_PLANNER','trace.read'),
    ('PMC_PLANNER','feedback.read'), ('PMC_PLANNER','feedback.create'),
    ('PMC_PLANNER','master.read'), ('PMC_PLANNER','process.read'),

    ('WORKSHOP_MANAGER','dashboard.read'), ('WORKSHOP_MANAGER','planning.read'),
    ('WORKSHOP_MANAGER','planning.work_order.dispatch'), ('WORKSHOP_MANAGER','warehouse.read'),
    ('WORKSHOP_MANAGER','production.read'),
    ('WORKSHOP_MANAGER','production.report.review'), ('WORKSHOP_MANAGER','production.wage.read_summary'),
    ('WORKSHOP_MANAGER','quality.read'), ('WORKSHOP_MANAGER','equipment.read'),
    ('WORKSHOP_MANAGER','equipment.fault.report'), ('WORKSHOP_MANAGER','trace.read'),
    ('WORKSHOP_MANAGER','feedback.read'), ('WORKSHOP_MANAGER','feedback.create'),
    ('WORKSHOP_MANAGER','master.read'), ('WORKSHOP_MANAGER','process.read'),

    ('PRODUCTION_OPERATOR','dashboard.read'), ('PRODUCTION_OPERATOR','planning.work_order.read'),
    ('PRODUCTION_OPERATOR','planning.work_order.receive'), ('PRODUCTION_OPERATOR','production.read'),
    ('PRODUCTION_OPERATOR','production.report.create'), ('PRODUCTION_OPERATOR','production.report.update_own'),
    ('PRODUCTION_OPERATOR','warehouse.requisition.create'),
    ('PRODUCTION_OPERATOR','production.wage.read_self'), ('PRODUCTION_OPERATOR','equipment.fault.report'),
    ('PRODUCTION_OPERATOR','equipment.read'), ('PRODUCTION_OPERATOR','feedback.read'),
    ('PRODUCTION_OPERATOR','feedback.create'),

    ('WAREHOUSE_ADMIN','dashboard.read'), ('WAREHOUSE_ADMIN','warehouse.read'),
    ('WAREHOUSE_ADMIN','warehouse.master.manage'), ('WAREHOUSE_ADMIN','warehouse.requisition.approve'),
    ('WAREHOUSE_ADMIN','warehouse.picking.execute'), ('WAREHOUSE_ADMIN','warehouse.delivery.execute'),
    ('WAREHOUSE_ADMIN','warehouse.inventory.adjust'), ('WAREHOUSE_ADMIN','planning.read'),
    ('WAREHOUSE_ADMIN','trace.read'), ('WAREHOUSE_ADMIN','feedback.read'),
    ('WAREHOUSE_ADMIN','feedback.create'), ('WAREHOUSE_ADMIN','master.read'),

    ('QUALITY_MANAGER','dashboard.read'), ('QUALITY_MANAGER','quality.read'),
    ('QUALITY_MANAGER','quality.inspection.create'), ('QUALITY_MANAGER','quality.inspection.assign'),
    ('QUALITY_MANAGER','quality.review'), ('QUALITY_MANAGER','quality.rework.manage'),
    ('QUALITY_MANAGER','planning.read'), ('QUALITY_MANAGER','trace.read'),
    ('QUALITY_MANAGER','feedback.read'), ('QUALITY_MANAGER','feedback.create'),
    ('QUALITY_MANAGER','master.read'), ('QUALITY_MANAGER','process.read'),

    ('QUALITY_INSPECTOR','dashboard.read'), ('QUALITY_INSPECTOR','quality.read'),
    ('QUALITY_INSPECTOR','quality.inspect'), ('QUALITY_INSPECTOR','equipment.fault.report'),
    ('QUALITY_INSPECTOR','equipment.read'), ('QUALITY_INSPECTOR','trace.read'),
    ('QUALITY_INSPECTOR','feedback.read'), ('QUALITY_INSPECTOR','feedback.create'),
    ('QUALITY_INSPECTOR','process.read'),

    ('PROCESS_ENGINEER','dashboard.read'), ('PROCESS_ENGINEER','process.read'),
    ('PROCESS_ENGINEER','process.manage'), ('PROCESS_ENGINEER','master.read'),
    ('PROCESS_ENGINEER','master.manage'), ('PROCESS_ENGINEER','planning.read'),
    ('PROCESS_ENGINEER','production.read'), ('PROCESS_ENGINEER','quality.read'),
    ('PROCESS_ENGINEER','equipment.read'), ('PROCESS_ENGINEER','trace.read'),
    ('PROCESS_ENGINEER','feedback.read'), ('PROCESS_ENGINEER','feedback.create'),

    ('EQUIPMENT_ADMIN','dashboard.read'), ('EQUIPMENT_ADMIN','equipment.read'),
    ('EQUIPMENT_ADMIN','equipment.manage'), ('EQUIPMENT_ADMIN','equipment.fault.report'),
    ('EQUIPMENT_ADMIN','equipment.repair.review'), ('EQUIPMENT_ADMIN','equipment.maintenance.assign'),
    ('EQUIPMENT_ADMIN','equipment.maintenance.accept'), ('EQUIPMENT_ADMIN','planning.read'),
    ('EQUIPMENT_ADMIN','trace.read'), ('EQUIPMENT_ADMIN','feedback.read'),
    ('EQUIPMENT_ADMIN','feedback.create'), ('EQUIPMENT_ADMIN','master.read'),

    ('EQUIPMENT_MAINTAINER','dashboard.read'), ('EQUIPMENT_MAINTAINER','equipment.read'),
    ('EQUIPMENT_MAINTAINER','equipment.maintenance.execute'),
    ('EQUIPMENT_MAINTAINER','feedback.read'), ('EQUIPMENT_MAINTAINER','feedback.create')
)
INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM grants g
JOIN mes_role r ON r.role_code = g.role_code
JOIN mes_permission p ON p.permission_code = g.permission_code
ON CONFLICT (role_id, permission_id) DO NOTHING;

DELETE FROM mes_role_permission rp
USING mes_role r
WHERE rp.role_id = r.role_id
  AND r.role_code = 'VIEWER';

UPDATE mes_role
SET enabled = 0
WHERE role_code = 'VIEWER';

-- 系统管理员始终拥有全部启用权限。
INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r CROSS JOIN mes_permission p
WHERE r.role_code = 'SYSTEM_ADMIN' AND p.enabled = 1
ON CONFLICT (role_id, permission_id) DO NOTHING;

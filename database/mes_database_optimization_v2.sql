-- MES database optimization v2.
-- Scope: RBAC, organization, audit, session, approval, process/quality/equipment extensions.
-- This script is additive and idempotent. It does not drop existing tables or columns.

CREATE TABLE IF NOT EXISTS mes_department (
    dept_id BIGSERIAL PRIMARY KEY,
    dept_code VARCHAR(50) NOT NULL,
    dept_name VARCHAR(100) NOT NULL,
    parent_dept_id BIGINT,
    dept_type VARCHAR(50),
    manager_user_id BIGINT,
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_department_dept_code ON mes_department (dept_code);
CREATE INDEX IF NOT EXISTS idx_mes_department_parent_dept_id ON mes_department (parent_dept_id);
COMMENT ON TABLE mes_department IS '组织部门表：存储部门、车间、仓库、质量、设备等组织单元。';
COMMENT ON COLUMN mes_department.dept_code IS '部门编码';
COMMENT ON COLUMN mes_department.dept_name IS '部门名称';
COMMENT ON COLUMN mes_department.parent_dept_id IS '上级部门';
COMMENT ON COLUMN mes_department.dept_type IS '部门类型';
COMMENT ON COLUMN mes_department.manager_user_id IS '负责人用户';

ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS employee_no VARCHAR(50);
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS position_name VARCHAR(100);
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS account_type VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE';
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS password_updated_at TIMESTAMP;
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS failed_login_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE mes_user ADD COLUMN IF NOT EXISTS updated_by BIGINT;
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_user_employee_no ON mes_user (employee_no) WHERE employee_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_mes_user_dept_id ON mes_user (dept_id);
COMMENT ON COLUMN mes_user.employee_no IS '员工工号';
COMMENT ON COLUMN mes_user.dept_id IS '所属部门';
COMMENT ON COLUMN mes_user.position_name IS '岗位名称';
COMMENT ON COLUMN mes_user.account_type IS '账号类型：EMPLOYEE/SYSTEM/API/ROBOT';
COMMENT ON COLUMN mes_user.failed_login_count IS '连续登录失败次数';
COMMENT ON COLUMN mes_user.locked_until IS '账号锁定截止时间';

CREATE TABLE IF NOT EXISTS mes_role (
    role_id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    role_type VARCHAR(30) NOT NULL DEFAULT 'BUSINESS',
    role_level INTEGER NOT NULL DEFAULT 100,
    data_scope VARCHAR(30) NOT NULL DEFAULT 'OWN',
    description VARCHAR(500),
    builtin SMALLINT NOT NULL DEFAULT 0,
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_role_role_code ON mes_role (role_code);
CREATE INDEX IF NOT EXISTS idx_mes_role_enabled ON mes_role (enabled);
COMMENT ON TABLE mes_role IS '角色表：存储系统管理员、PMC、车间、仓库、质量、工艺、设备、人事等角色。';
COMMENT ON COLUMN mes_role.data_scope IS '数据范围：ALL/DEPT/LINE/OWN/CUSTOM';

CREATE TABLE IF NOT EXISTS mes_permission (
    permission_id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(120) NOT NULL,
    permission_name VARCHAR(120) NOT NULL,
    module_code VARCHAR(50) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    action_code VARCHAR(40) NOT NULL,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    description VARCHAR(500),
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_permission_permission_code ON mes_permission (permission_code);
CREATE INDEX IF NOT EXISTS idx_mes_permission_module_action ON mes_permission (module_code, action_code);
COMMENT ON TABLE mes_permission IS '权限点表：存储模块资源动作粒度的权限。';

CREATE TABLE IF NOT EXISTS mes_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    granted_by BIGINT,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);
CREATE INDEX IF NOT EXISTS idx_mes_role_permission_permission_id ON mes_role_permission (permission_id);
COMMENT ON TABLE mes_role_permission IS '角色权限关联表。';

CREATE TABLE IF NOT EXISTS mes_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS idx_mes_user_role_role_id ON mes_user_role (role_id);
COMMENT ON TABLE mes_user_role IS '用户角色关联表：支持一个用户多个角色，兼容 mes_user.role_code。';

CREATE TABLE IF NOT EXISTS mes_role_data_scope (
    scope_id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    scope_value VARCHAR(100),
    description VARCHAR(300),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mes_role_data_scope_role_id ON mes_role_data_scope (role_id);
COMMENT ON TABLE mes_role_data_scope IS '角色数据范围表：按部门、产线、仓库等限制数据可见范围。';

CREATE TABLE IF NOT EXISTS mes_permission_apply (
    apply_id BIGSERIAL PRIMARY KEY,
    apply_no VARCHAR(50) NOT NULL,
    applicant_id BIGINT,
    target_user_id BIGINT NOT NULL,
    from_role_code VARCHAR(50),
    to_role_code VARCHAR(50) NOT NULL,
    apply_reason VARCHAR(500),
    apply_status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reviewer_id BIGINT,
    reviewed_at TIMESTAMP,
    review_comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_permission_apply_apply_no ON mes_permission_apply (apply_no);
CREATE INDEX IF NOT EXISTS idx_mes_permission_apply_target_user_id ON mes_permission_apply (target_user_id);
CREATE INDEX IF NOT EXISTS idx_mes_permission_apply_status ON mes_permission_apply (apply_status);
COMMENT ON TABLE mes_permission_apply IS '权限变更申请表：人事经理发起，系统管理员处理。';

CREATE TABLE IF NOT EXISTS mes_user_session (
    session_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    login_ip VARCHAR(64),
    user_agent VARCHAR(500),
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_user_session_token_hash ON mes_user_session (token_hash);
CREATE INDEX IF NOT EXISTS idx_mes_user_session_user_id ON mes_user_session (user_id);
CREATE INDEX IF NOT EXISTS idx_mes_user_session_expires_at ON mes_user_session (expires_at);
COMMENT ON TABLE mes_user_session IS '用户登录会话表：用于后端可校验、可过期、可撤销的登录状态。';

CREATE TABLE IF NOT EXISTS mes_audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    module_code VARCHAR(50),
    action_code VARCHAR(50),
    resource_type VARCHAR(80),
    resource_id VARCHAR(80),
    user_id BIGINT,
    username VARCHAR(50),
    role_code VARCHAR(50),
    request_method VARCHAR(20),
    request_path VARCHAR(300),
    client_ip VARCHAR(64),
    before_data JSONB,
    after_data JSONB,
    result VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mes_audit_log_user_id ON mes_audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_mes_audit_log_event_type ON mes_audit_log (event_type);
CREATE INDEX IF NOT EXISTS idx_mes_audit_log_created_at ON mes_audit_log (created_at);
CREATE INDEX IF NOT EXISTS idx_mes_audit_log_resource ON mes_audit_log (resource_type, resource_id);
COMMENT ON TABLE mes_audit_log IS '审计日志表：记录登录、权限变更、删除、审批、库存调整、质量判定等关键动作。';

CREATE TABLE IF NOT EXISTS mes_api_client (
    client_id BIGSERIAL PRIMARY KEY,
    client_code VARCHAR(80) NOT NULL,
    client_name VARCHAR(120) NOT NULL,
    client_type VARCHAR(40) NOT NULL,
    token_hash VARCHAR(255),
    allowed_scopes VARCHAR(1000),
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_api_client_client_code ON mes_api_client (client_code);
COMMENT ON TABLE mes_api_client IS '系统集成客户端表：管理机器人、WMS、财务系统等非人工账号。';

CREATE TABLE IF NOT EXISTS mes_approval_record (
    approval_id BIGSERIAL PRIMARY KEY,
    business_type VARCHAR(80) NOT NULL,
    business_id BIGINT NOT NULL,
    approval_node VARCHAR(80) NOT NULL,
    approver_id BIGINT,
    approval_result VARCHAR(30) NOT NULL,
    approval_comment VARCHAR(500),
    approved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mes_approval_record_business ON mes_approval_record (business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_mes_approval_record_approver_id ON mes_approval_record (approver_id);
COMMENT ON TABLE mes_approval_record IS '通用审批记录表：记录报工、领料、质检、维修、权限申请等审批过程。';

CREATE TABLE IF NOT EXISTS mes_notification (
    notification_id BIGSERIAL PRIMARY KEY,
    receiver_user_id BIGINT,
    receiver_role_code VARCHAR(50),
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000),
    business_type VARCHAR(80),
    business_id BIGINT,
    read_status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mes_notification_receiver_user_id ON mes_notification (receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_mes_notification_receiver_role ON mes_notification (receiver_role_code);
COMMENT ON TABLE mes_notification IS '系统通知表：用于缺料、报工审核、质检任务、维修派工等消息提醒。';

CREATE TABLE IF NOT EXISTS mes_sop (
    sop_id BIGSERIAL PRIMARY KEY,
    sop_code VARCHAR(50) NOT NULL,
    sop_name VARCHAR(150) NOT NULL,
    product_id BIGINT,
    process_id BIGINT,
    version_no VARCHAR(30) NOT NULL DEFAULT 'V1.0',
    sop_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    content TEXT,
    attachment_url VARCHAR(500),
    created_by BIGINT,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_sop_code_version ON mes_sop (sop_code, version_no);
CREATE INDEX IF NOT EXISTS idx_mes_sop_product_process ON mes_sop (product_id, process_id);
COMMENT ON TABLE mes_sop IS 'SOP标准作业指导书表：由工艺工程师维护。';

CREATE TABLE IF NOT EXISTS mes_defect_reason (
    reason_id BIGSERIAL PRIMARY KEY,
    reason_code VARCHAR(50) NOT NULL,
    reason_name VARCHAR(150) NOT NULL,
    process_id BIGINT,
    defect_type VARCHAR(80),
    severity_level VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_defect_reason_reason_code ON mes_defect_reason (reason_code);
CREATE INDEX IF NOT EXISTS idx_mes_defect_reason_process_id ON mes_defect_reason (process_id);
COMMENT ON TABLE mes_defect_reason IS '工序不良原因表：用于质检判定、返工原因和质量统计。';

CREATE TABLE IF NOT EXISTS mes_process_parameter (
    parameter_id BIGSERIAL PRIMARY KEY,
    parameter_code VARCHAR(50) NOT NULL,
    parameter_name VARCHAR(150) NOT NULL,
    product_id BIGINT,
    process_id BIGINT,
    standard_value VARCHAR(100),
    upper_limit NUMERIC(18,4),
    lower_limit NUMERIC(18,4),
    unit VARCHAR(30),
    version_no VARCHAR(30) NOT NULL DEFAULT 'V1.0',
    enabled SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_process_parameter_code_version ON mes_process_parameter (parameter_code, version_no);
CREATE INDEX IF NOT EXISTS idx_mes_process_parameter_product_process ON mes_process_parameter (product_id, process_id);
COMMENT ON TABLE mes_process_parameter IS '工艺参数表：维护轮胎用料和工序控制参数。';

CREATE TABLE IF NOT EXISTS mes_quality_standard (
    standard_id BIGSERIAL PRIMARY KEY,
    standard_code VARCHAR(50) NOT NULL,
    standard_name VARCHAR(150) NOT NULL,
    product_id BIGINT,
    process_id BIGINT,
    inspection_type VARCHAR(50),
    version_no VARCHAR(30) NOT NULL DEFAULT 'V1.0',
    standard_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mes_quality_standard_code_version ON mes_quality_standard (standard_code, version_no);
CREATE INDEX IF NOT EXISTS idx_mes_quality_standard_product_process ON mes_quality_standard (product_id, process_id);
COMMENT ON TABLE mes_quality_standard IS '质量检验标准表：由质量主管维护和审核。';

CREATE TABLE IF NOT EXISTS mes_quality_standard_item (
    standard_item_id BIGSERIAL PRIMARY KEY,
    standard_id BIGINT NOT NULL,
    item_code VARCHAR(50) NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    standard_value VARCHAR(100),
    upper_limit NUMERIC(18,4),
    lower_limit NUMERIC(18,4),
    unit VARCHAR(30),
    sampling_rule VARCHAR(200),
    required SMALLINT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_mes_quality_standard_item_standard_id ON mes_quality_standard_item (standard_id);
COMMENT ON TABLE mes_quality_standard_item IS '质量检验标准项目表：定义抽检项目、标准值和上下限。';

CREATE TABLE IF NOT EXISTS mes_equipment_status_log (
    status_log_id BIGSERIAL PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by BIGINT,
    changed_reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mes_equipment_status_log_equipment_id ON mes_equipment_status_log (equipment_id);
CREATE INDEX IF NOT EXISTS idx_mes_equipment_status_log_changed_at ON mes_equipment_status_log (changed_at);
COMMENT ON TABLE mes_equipment_status_log IS '设备状态履历表：记录设备状态变化全过程。';

ALTER TABLE mes_customer_order ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE mes_customer_order ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE mes_customer_order ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE mes_production_task ADD COLUMN IF NOT EXISTS created_by BIGINT;
ALTER TABLE mes_production_task ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE mes_production_task ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE mes_work_order ADD COLUMN IF NOT EXISTS assigned_to BIGINT;
ALTER TABLE mes_work_order ADD COLUMN IF NOT EXISTS accepted_by BIGINT;
ALTER TABLE mes_work_order ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP;
ALTER TABLE mes_work_order ADD COLUMN IF NOT EXISTS updated_by BIGINT;
ALTER TABLE mes_work_order ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE mes_material_requisition ADD COLUMN IF NOT EXISTS approved_by BIGINT;
ALTER TABLE mes_material_requisition ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE mes_work_report ADD COLUMN IF NOT EXISTS approved_by BIGINT;
ALTER TABLE mes_work_report ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE mes_quality_inspection ADD COLUMN IF NOT EXISTS assigned_to BIGINT;
ALTER TABLE mes_quality_inspection ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE mes_quality_inspection ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE mes_rework_order ADD COLUMN IF NOT EXISTS approved_by BIGINT;
ALTER TABLE mes_rework_order ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE mes_equipment_repair_report ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE mes_equipment_repair_report ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE mes_maintenance_order ADD COLUMN IF NOT EXISTS accepted_by BIGINT;
ALTER TABLE mes_maintenance_order ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP;
ALTER TABLE mes_maintenance_order ADD COLUMN IF NOT EXISTS verified_by BIGINT;
ALTER TABLE mes_maintenance_order ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
ALTER TABLE mes_management_feedback ADD COLUMN IF NOT EXISTS handled_by BIGINT;
ALTER TABLE mes_management_feedback ADD COLUMN IF NOT EXISTS handled_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_mes_work_order_assigned_to ON mes_work_order (assigned_to);
CREATE INDEX IF NOT EXISTS idx_mes_work_order_accepted_by ON mes_work_order (accepted_by);
CREATE INDEX IF NOT EXISTS idx_mes_work_report_approved_by ON mes_work_report (approved_by);
CREATE INDEX IF NOT EXISTS idx_mes_quality_inspection_assigned_to ON mes_quality_inspection (assigned_to);
CREATE INDEX IF NOT EXISTS idx_mes_quality_inspection_reviewed_by ON mes_quality_inspection (reviewed_by);
CREATE INDEX IF NOT EXISTS idx_mes_maintenance_order_accepted_by ON mes_maintenance_order (accepted_by);
CREATE INDEX IF NOT EXISTS idx_mes_maintenance_order_verified_by ON mes_maintenance_order (verified_by);

INSERT INTO mes_department (dept_code, dept_name, dept_type)
VALUES
    ('ADMIN', '系统管理部', 'SYSTEM'),
    ('PMC', '计划管理部', 'PLANNING'),
    ('WORKSHOP', '生产车间', 'WORKSHOP'),
    ('WAREHOUSE', '仓储物流部', 'WAREHOUSE'),
    ('QUALITY', '质量管理部', 'QUALITY'),
    ('PROCESS', '工艺工程部', 'PROCESS'),
    ('EQUIPMENT', '设备管理部', 'EQUIPMENT'),
    ('HR', '人事部', 'HR'),
    ('MANAGEMENT', '经营管理层', 'MANAGEMENT')
ON CONFLICT (dept_code) DO NOTHING;

INSERT INTO mes_role (role_code, role_name, role_type, role_level, data_scope, builtin, description)
VALUES
    ('SYSTEM_ADMIN', '系统管理员', 'SYSTEM', 1, 'ALL', 1, '最高系统权限'),
    ('HR_MANAGER', '人事经理', 'BUSINESS', 30, 'DEPT', 1, '员工信息维护和权限申请'),
    ('GENERAL_MANAGER', '总经理/管理层', 'MANAGEMENT', 20, 'ALL', 1, '经营看板和决策查询'),
    ('PMC_PLANNER', 'PMC计划员', 'BUSINESS', 50, 'DEPT', 1, '订单、计划、齐套、工单'),
    ('WORKSHOP_MANAGER', '车间管理员', 'BUSINESS', 50, 'DEPT', 1, '车间执行和报工审核'),
    ('PRODUCTION_OPERATOR', '生产操作工', 'BUSINESS', 80, 'OWN', 1, '工单执行和生产报工'),
    ('WAREHOUSE_ADMIN', '仓库管理员', 'BUSINESS', 60, 'DEPT', 1, '仓储、库存、领料、配送'),
    ('QUALITY_MANAGER', '质量主管', 'BUSINESS', 50, 'DEPT', 1, '质检标准、任务分配、结果审核'),
    ('QUALITY_INSPECTOR', '质检员', 'BUSINESS', 70, 'OWN', 1, '质检执行和结果录入'),
    ('PROCESS_ENGINEER', '工艺工程师', 'BUSINESS', 60, 'DEPT', 1, 'SOP、工艺参数、不良原因'),
    ('EQUIPMENT_ADMIN', '设备管理员', 'BUSINESS', 50, 'DEPT', 1, '设备台账、状态、维修派工'),
    ('EQUIPMENT_MAINTAINER', '设备维护员', 'BUSINESS', 70, 'OWN', 1, '维修执行和维护记录')
ON CONFLICT (role_code) DO NOTHING;

INSERT INTO mes_permission (permission_code, permission_name, module_code, resource_type, action_code, risk_level)
VALUES
    ('user.read', '查看用户', 'system', 'user', 'read', 'MEDIUM'),
    ('user.create', '创建用户', 'system', 'user', 'create', 'HIGH'),
    ('user.update_role', '修改用户角色', 'system', 'user', 'update_role', 'HIGH'),
    ('permission.apply', '发起权限申请', 'system', 'permission_apply', 'create', 'MEDIUM'),
    ('permission.review', '审核权限申请', 'system', 'permission_apply', 'review', 'HIGH'),
    ('audit.read', '查看审计日志', 'system', 'audit_log', 'read', 'HIGH'),
    ('planning.manage', '管理计划工单', 'planning', 'planning', 'manage', 'MEDIUM'),
    ('warehouse.manage', '管理仓储库存', 'warehouse', 'warehouse', 'manage', 'HIGH'),
    ('production.report', '提交生产报工', 'production', 'work_report', 'create', 'MEDIUM'),
    ('production.report.review', '审核生产报工', 'production', 'work_report', 'review', 'HIGH'),
    ('quality.inspect', '执行质检', 'quality', 'inspection', 'execute', 'MEDIUM'),
    ('quality.review', '审核质检结果', 'quality', 'inspection', 'review', 'HIGH'),
    ('process.manage', '维护工艺标准', 'process', 'process_standard', 'manage', 'HIGH'),
    ('equipment.manage', '管理设备台账', 'equipment', 'equipment', 'manage', 'HIGH'),
    ('equipment.maintain', '执行设备维修', 'equipment', 'maintenance_order', 'execute', 'MEDIUM'),
    ('dashboard.read', '查看综合看板', 'dashboard', 'dashboard', 'read', 'LOW'),
    ('trace.read', '查看产品追溯', 'trace', 'trace', 'read', 'LOW')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN (
    'dashboard.read', 'user.read', 'user.create', 'user.update_role',
    'permission.review', 'audit.read'
)
WHERE r.role_code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('user.read', 'permission.apply', 'dashboard.read')
WHERE r.role_code = 'HR_MANAGER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('dashboard.read', 'trace.read')
WHERE r.role_code IN ('GENERAL_MANAGER')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('planning.manage', 'dashboard.read', 'trace.read')
WHERE r.role_code = 'PMC_PLANNER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('production.report.review', 'dashboard.read', 'trace.read')
WHERE r.role_code = 'WORKSHOP_MANAGER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('production.report', 'dashboard.read')
WHERE r.role_code = 'PRODUCTION_OPERATOR'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('warehouse.manage', 'dashboard.read')
WHERE r.role_code = 'WAREHOUSE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('quality.inspect', 'quality.review', 'dashboard.read', 'trace.read')
WHERE r.role_code = 'QUALITY_MANAGER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('quality.inspect', 'trace.read')
WHERE r.role_code = 'QUALITY_INSPECTOR'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('process.manage', 'trace.read')
WHERE r.role_code = 'PROCESS_ENGINEER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('equipment.manage', 'dashboard.read', 'trace.read')
WHERE r.role_code = 'EQUIPMENT_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM mes_role r
JOIN mes_permission p ON p.permission_code IN ('equipment.maintain', 'dashboard.read')
WHERE r.role_code = 'EQUIPMENT_MAINTAINER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO mes_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM mes_user u
JOIN mes_role r ON r.role_code = 'SYSTEM_ADMIN'
WHERE u.username = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;

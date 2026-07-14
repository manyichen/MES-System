-- MES role acceptance accounts
-- All acceptance accounts use the shared development password: 123456.

WITH accounts(username, real_name, role_code, department, password_hash) AS (
    VALUES
    ('admin', '系统管理员', 'SYSTEM_ADMIN', '信息技术部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_hr', '人事经理验收员', 'HR_MANAGER', '人事部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_general', '总经理验收员', 'GENERAL_MANAGER', '经营管理层', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_pmc', 'PMC计划验收员', 'PMC_PLANNER', '生产计划部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_workshop', '车间管理验收员', 'WORKSHOP_MANAGER', '生产车间', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_operator', '生产操作验收员', 'PRODUCTION_OPERATOR', '生产车间', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_warehouse', '仓库管理验收员', 'WAREHOUSE_ADMIN', '仓储物流部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_quality_mgr', '质量主管验收员', 'QUALITY_MANAGER', '质量部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_inspector', '质检员验收员', 'QUALITY_INSPECTOR', '质量部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_process', '工艺工程验收员', 'PROCESS_ENGINEER', '工艺技术部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_equipment_mgr', '设备管理验收员', 'EQUIPMENT_ADMIN', '设备部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_maintainer', '设备维修验收员', 'EQUIPMENT_MAINTAINER', '设备部', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8=')
)
INSERT INTO mes_user
    (username, real_name, role_code, department, enabled, password_hash,
     failed_login_count, locked_until, updated_at)
SELECT username, real_name, role_code, department, 1, password_hash,
       0, NULL, CURRENT_TIMESTAMP
FROM accounts
ON CONFLICT (username) DO UPDATE SET
    real_name = EXCLUDED.real_name,
    role_code = EXCLUDED.role_code,
    department = EXCLUDED.department,
    enabled = 1,
    password_hash = EXCLUDED.password_hash,
    failed_login_count = 0,
    locked_until = NULL,
    updated_at = CURRENT_TIMESTAMP;

DELETE FROM mes_user_session
WHERE user_id IN (
    SELECT user_id
    FROM mes_user
    WHERE username IN ('mes_sysmaint', 'mes_viewer')
);

DELETE FROM mes_user_role
WHERE user_id IN (
    SELECT user_id
    FROM mes_user
    WHERE username IN ('mes_sysmaint', 'mes_viewer')
);

DELETE FROM mes_user
WHERE username IN ('mes_sysmaint', 'mes_viewer');

DELETE FROM mes_user_role ur
USING mes_user u
WHERE ur.user_id = u.user_id
  AND u.username IN (
      'admin', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
      'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
      'mes_process', 'mes_equipment_mgr', 'mes_maintainer'
  );

INSERT INTO mes_user_role (user_id, role_id, assigned_at)
SELECT u.user_id, r.role_id, CURRENT_TIMESTAMP
FROM mes_user u
JOIN mes_role r ON r.role_code = u.role_code AND r.enabled = 1
WHERE u.username IN (
    'admin', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
    'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
    'mes_process', 'mes_equipment_mgr', 'mes_maintainer'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

UPDATE mes_user_session
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL
  AND user_id IN (
      SELECT user_id FROM mes_user
      WHERE username IN (
          'admin', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
          'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
          'mes_process', 'mes_equipment_mgr', 'mes_maintainer'
      )
  );

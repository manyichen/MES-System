-- Super administrator role and permission grant.
-- The account itself is provisioned by DatabaseMigrationRunner using
-- MES_SUPER_ADMIN_USERNAME (optional) and MES_SUPER_ADMIN_PASSWORD (required).

INSERT INTO mes_role
    (role_code, role_name, role_type, role_level, data_scope, builtin, enabled, description, updated_at)
VALUES
    ('SUPER_ADMIN', '超级管理员', 'SYSTEM', 0, 'ALL', 1, 1,
     '拥有系统内全部已启用功能、权限和全量数据范围', CURRENT_TIMESTAMP)
ON CONFLICT (role_code) DO UPDATE
SET role_name = EXCLUDED.role_name,
    role_type = EXCLUDED.role_type,
    role_level = EXCLUDED.role_level,
    data_scope = EXCLUDED.data_scope,
    builtin = EXCLUDED.builtin,
    enabled = EXCLUDED.enabled,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO mes_role_permission (role_id, permission_id, granted_by, granted_at)
SELECT super_role.role_id,
       permission.permission_id,
       (SELECT MIN(user_id) FROM mes_user WHERE role_code IN ('SYSTEM_ADMIN', 'SUPER_ADMIN')),
       CURRENT_TIMESTAMP
FROM mes_role super_role
CROSS JOIN mes_permission permission
WHERE super_role.role_code = 'SUPER_ADMIN'
  AND super_role.enabled = 1
  AND permission.enabled = 1
ON CONFLICT (role_id, permission_id) DO UPDATE
SET granted_at = EXCLUDED.granted_at;

-- Align deployed databases with the consolidated API and the formal 12-role model.
BEGIN;

UPDATE mes_shortage_alert
SET receiver_role = 'WAREHOUSE_ADMIN'
WHERE receiver_role = 'WAREHOUSE_KEEPER';

UPDATE mes_user
SET role_code = 'WAREHOUSE_ADMIN', updated_at = CURRENT_TIMESTAMP
WHERE role_code = 'WAREHOUSE_KEEPER';

INSERT INTO mes_user_role (user_id, role_id)
SELECT ur.user_id, target.role_id
FROM mes_user_role ur
JOIN mes_role legacy ON legacy.role_id = ur.role_id AND legacy.role_code = 'WAREHOUSE_KEEPER'
JOIN mes_role target ON target.role_code = 'WAREHOUSE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

DELETE FROM mes_user_role ur
USING mes_role role
WHERE ur.role_id = role.role_id
  AND role.role_code = 'WAREHOUSE_KEEPER';

DELETE FROM mes_role_permission rp
USING mes_permission permission
WHERE rp.permission_id = permission.permission_id
  AND permission.permission_code = 'dashboard.system.read';

DELETE FROM mes_permission WHERE permission_code = 'dashboard.system.read';
DELETE FROM mes_role WHERE role_code = 'WAREHOUSE_KEEPER';

COMMIT;

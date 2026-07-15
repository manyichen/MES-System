-- Remove warehouse logistics access from workshop managers.
DELETE FROM mes_role_permission rp
USING mes_role r, mes_permission p
WHERE rp.role_id = r.role_id
  AND rp.permission_id = p.permission_id
  AND r.role_code = 'WORKSHOP_MANAGER'
  AND p.permission_code LIKE 'warehouse.%';

DELETE FROM mes_user_warehouse_scope uws
WHERE EXISTS (
    SELECT 1
    FROM mes_user_role ur
    JOIN mes_role r ON r.role_id = ur.role_id
    WHERE ur.user_id = uws.user_id
      AND r.role_code = 'WORKSHOP_MANAGER'
)
OR EXISTS (
    SELECT 1
    FROM mes_user u
    WHERE u.user_id = uws.user_id
      AND u.role_code = 'WORKSHOP_MANAGER'
);

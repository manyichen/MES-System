-- Keep only the merged system administrator role.
-- The surviving role code is SYSTEM_ADMIN. Former SYSTEM_MAINTAINER users are
-- reassigned to SYSTEM_ADMIN, then the old role and its grants are removed.

WITH admin_role AS (
    SELECT role_id
    FROM mes_role
    WHERE role_code = 'SYSTEM_ADMIN'
),
maintainer_role AS (
    SELECT role_id
    FROM mes_role
    WHERE role_code = 'SYSTEM_MAINTAINER'
)
INSERT INTO mes_user_role (user_id, role_id, assigned_at)
SELECT ur.user_id, admin_role.role_id, CURRENT_TIMESTAMP
FROM mes_user_role ur
CROSS JOIN admin_role
JOIN maintainer_role ON maintainer_role.role_id = ur.role_id
ON CONFLICT (user_id, role_id) DO NOTHING;

UPDATE mes_user_session
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL
  AND user_id IN (
      SELECT ur.user_id
      FROM mes_user_role ur
      JOIN mes_role r ON r.role_id = ur.role_id
      WHERE r.role_code = 'SYSTEM_MAINTAINER'
  );

UPDATE mes_user
SET role_code = 'SYSTEM_ADMIN',
    updated_at = CURRENT_TIMESTAMP
WHERE role_code = 'SYSTEM_MAINTAINER';

DELETE FROM mes_user_session
WHERE user_id IN (
    SELECT user_id
    FROM mes_user
    WHERE username = 'mes_sysmaint'
);

DELETE FROM mes_user_role
WHERE user_id IN (
    SELECT user_id
    FROM mes_user
    WHERE username = 'mes_sysmaint'
);

DELETE FROM mes_user
WHERE username = 'mes_sysmaint';

DELETE FROM mes_user_role
WHERE role_id IN (
    SELECT role_id
    FROM mes_role
    WHERE role_code = 'SYSTEM_MAINTAINER'
);

DELETE FROM mes_role_permission
WHERE role_id IN (
    SELECT role_id
    FROM mes_role
    WHERE role_code = 'SYSTEM_MAINTAINER'
);

DELETE FROM mes_role_data_scope
WHERE role_id IN (
    SELECT role_id
    FROM mes_role
    WHERE role_code = 'SYSTEM_MAINTAINER'
);

DELETE FROM mes_role
WHERE role_code = 'SYSTEM_MAINTAINER';

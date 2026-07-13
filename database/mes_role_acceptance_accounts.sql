-- MES role acceptance accounts
-- All acceptance accounts use the shared development password: 123456.

WITH accounts(username, real_name, role_code, department, password_hash) AS (
    VALUES
    ('admin', '绯荤粺绠＄悊鍛?, 'SYSTEM_ADMIN', '淇℃伅鎶€鏈儴', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_sysmaint', '绯荤粺缁存姢楠屾敹鍛?, 'SYSTEM_MAINTAINER', '淇℃伅鎶€鏈儴', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_hr', '浜轰簨缁忕悊楠屾敹鍛?, 'HR_MANAGER', '浜轰簨閮?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_general', '鎬荤粡鐞嗛獙鏀跺憳', 'GENERAL_MANAGER', '缁忚惀绠＄悊灞?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_pmc', 'PMC璁″垝楠屾敹鍛?, 'PMC_PLANNER', '鐢熶骇璁″垝閮?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_workshop', '杞﹂棿绠＄悊楠屾敹鍛?, 'WORKSHOP_MANAGER', '鐢熶骇杞﹂棿', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_operator', '鐢熶骇鎿嶄綔楠屾敹鍛?, 'PRODUCTION_OPERATOR', '鐢熶骇杞﹂棿', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_warehouse', '浠撳簱绠＄悊楠屾敹鍛?, 'WAREHOUSE_ADMIN', '浠撳偍鐗╂祦閮?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_quality_mgr', '璐ㄩ噺涓荤楠屾敹鍛?, 'QUALITY_MANAGER', '璐ㄩ噺閮?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_inspector', '璐ㄦ鍛橀獙鏀跺憳', 'QUALITY_INSPECTOR', '璐ㄩ噺閮?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_process', '宸ヨ壓宸ョ▼楠屾敹鍛?, 'PROCESS_ENGINEER', '宸ヨ壓鎶€鏈儴', 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_equipment_mgr', '璁惧绠＄悊楠屾敹鍛?, 'EQUIPMENT_ADMIN', '璁惧閮?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8='),
    ('mes_maintainer', '璁惧缁翠慨楠屾敹鍛?, 'EQUIPMENT_MAINTAINER', '璁惧閮?, 'pbkdf2_sha256$120000$SJwp/esDPi+QeXO5mzSm8g==$4jC2gVqoKr905JSM9t4KRahNrTicPNMYRWHvZ6jTGR8=')
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

DELETE FROM mes_user_role ur
USING mes_user u
WHERE ur.user_id = u.user_id
  AND u.username IN (
      'admin', 'mes_sysmaint', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
      'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
      'mes_process', 'mes_equipment_mgr', 'mes_maintainer'
  );

INSERT INTO mes_user_role (user_id, role_id, assigned_at)
SELECT u.user_id, r.role_id, CURRENT_TIMESTAMP
FROM mes_user u
JOIN mes_role r ON r.role_code = u.role_code AND r.enabled = 1
WHERE u.username IN (
    'admin', 'mes_sysmaint', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
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
          'admin', 'mes_sysmaint', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
          'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
          'mes_process', 'mes_equipment_mgr', 'mes_maintainer'
      )
  );

UPDATE mes_user
SET enabled = 0, updated_at = CURRENT_TIMESTAMP
WHERE username = 'mes_viewer' OR role_code = 'VIEWER';

DELETE FROM mes_user_role ur
USING mes_user u
WHERE ur.user_id = u.user_id
  AND (u.username = 'mes_viewer' OR u.role_code = 'VIEWER');

UPDATE mes_user_session
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL
  AND user_id IN (
      SELECT user_id FROM mes_user
      WHERE username = 'mes_viewer' OR role_code = 'VIEWER'
  );

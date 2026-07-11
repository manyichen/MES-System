-- MES role acceptance accounts
-- Plaintext passwords are intentionally not stored in this repository.

WITH accounts(username, real_name, role_code, department, password_hash) AS (
    VALUES
    ('mes_sysmaint', '系统维护验收员', 'SYSTEM_MAINTAINER', '信息技术部', 'pbkdf2_sha256$120000$8YNAEx2E906wct7tVyMxUg==$UzIldT9GW6gDLHSp6tRtp3J2gQoPgpl1zpMnVV27eno='),
    ('mes_hr', '人事经理验收员', 'HR_MANAGER', '人事部', 'pbkdf2_sha256$120000$m2pjzAd+4jYNnmrcMVIUTA==$28lIR7ji/9jp/tfZ6aRtv8HsHAtFNtRbESuzfy4rWlA='),
    ('mes_general', '总经理验收员', 'GENERAL_MANAGER', '经营管理层', 'pbkdf2_sha256$120000$MEFzm2hpmnsgp9ABewWSAA==$aGbcDTokeHZo1rqA5V9cguXO3GbOiYeobTaDDDVk09o='),
    ('mes_pmc', 'PMC计划验收员', 'PMC_PLANNER', '生产计划部', 'pbkdf2_sha256$120000$xnQ+ic/dGTZ3IXxNHBabTQ==$B7Pj2nBOTEvWggr2n1CALoaI9A/90Ws8/oMqVbA0uIM='),
    ('mes_workshop', '车间管理验收员', 'WORKSHOP_MANAGER', '生产车间', 'pbkdf2_sha256$120000$2EoaTCT/mQX/0tiD+CYelw==$Gpe2Sip+jrLNpBR/9aYf1YQvtVoxCbwrT0olz53d24I='),
    ('mes_operator', '生产操作验收员', 'PRODUCTION_OPERATOR', '生产车间', 'pbkdf2_sha256$120000$k6NWG1+c88pwYHqOtx/Tzw==$+eDG39fZn7r20ZkPLfKK/3jPL18fzgb0fnADvKndNeU='),
    ('mes_warehouse', '仓库管理验收员', 'WAREHOUSE_ADMIN', '仓储物流部', 'pbkdf2_sha256$120000$Ki9Lw2DcTLNXDFh0vLvCVg==$7KcvQtwGATbsDzO5hVPtEycV0R923A5+tw33OoSgXUc='),
    ('mes_quality_mgr', '质量主管验收员', 'QUALITY_MANAGER', '质量部', 'pbkdf2_sha256$120000$Eh0xt6lbVH2/WS3Sd5bEyA==$rDKp1DiBXH0xhyM3BL4Gtyfab785z7hHlbotwNG9gJQ='),
    ('mes_inspector', '质检员验收员', 'QUALITY_INSPECTOR', '质量部', 'pbkdf2_sha256$120000$jVHm7TM3Eo9C5GgsqMLvfA==$FJfONMh/G/5Ax70gGTKpiu9YPQTlDJrz1lfJpkoykt8='),
    ('mes_process', '工艺工程验收员', 'PROCESS_ENGINEER', '工艺技术部', 'pbkdf2_sha256$120000$86ZHe6xOTXRuS2GHWkMDHw==$Ylfyn9+JgjxPgM4gztAAj/da2Qd3mmTjHG7O82fNzec='),
    ('mes_equipment_mgr', '设备管理验收员', 'EQUIPMENT_ADMIN', '设备部', 'pbkdf2_sha256$120000$i3kj4i/0uxBzhxC0CY/pgQ==$z/nKdljkBUcHIrmBPhgL76M0mqBqP450aPP/A3XvNcs='),
    ('mes_maintainer', '设备维修验收员', 'EQUIPMENT_MAINTAINER', '设备部', 'pbkdf2_sha256$120000$z+0dvED2H/PBa60g2R9zhg==$EbyV+osM0mxKpJ7BJe71lhdwT+hk8kOZxZyP3ZXD9qk='),
    ('mes_viewer', '只读访客验收员', 'VIEWER', '访客', 'pbkdf2_sha256$120000$93JhUEoNC483yONTsVrZbw==$T0liEb0lwdrwSBBUaFpVv+qJZS2D28za5BWxiPeDG5k=')
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
      'mes_sysmaint', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
      'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
      'mes_process', 'mes_equipment_mgr', 'mes_maintainer', 'mes_viewer'
  );

INSERT INTO mes_user_role (user_id, role_id, assigned_at)
SELECT u.user_id, r.role_id, CURRENT_TIMESTAMP
FROM mes_user u
JOIN mes_role r ON r.role_code = u.role_code AND r.enabled = 1
WHERE u.username IN (
    'mes_sysmaint', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
    'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
    'mes_process', 'mes_equipment_mgr', 'mes_maintainer', 'mes_viewer'
)
ON CONFLICT (user_id, role_id) DO NOTHING;

UPDATE mes_user_session
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL
  AND user_id IN (
      SELECT user_id FROM mes_user
      WHERE username IN (
          'mes_sysmaint', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop',
          'mes_operator', 'mes_warehouse', 'mes_quality_mgr', 'mes_inspector',
          'mes_process', 'mes_equipment_mgr', 'mes_maintainer', 'mes_viewer'
      )
  );

-- 清除验收登录产生的临时会话，并重新压缩审计事件。
DELETE FROM mes_user_session;

DELETE FROM mes_audit_log
WHERE audit_id IN (
    SELECT audit_id FROM (
        SELECT audit_id, row_number() OVER (PARTITION BY event_type, result ORDER BY audit_id DESC) AS rn
        FROM mes_audit_log
    ) ranked WHERE rn > 2
);
